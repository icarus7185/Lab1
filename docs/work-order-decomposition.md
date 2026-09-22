# WO-201: Work-Order Decomposition

## 1. Summary

WO-201 adds a workflow for splitting one existing work order into a set of executable child work orders. The parent remains the source of truth for the requested outcome; each child captures one independently assignable unit of work.

The operation is atomic: either all requested children are created, or no child is persisted. Repeating the same request with the same idempotency key returns the original result without creating duplicates.

## 2. Scope and Non-Goals

### In scope

- Displaying a decomposition form for an existing work order.
- Validating child titles, descriptions, estimates, priorities, and due dates before submission.
- Creating child work orders under a parent work order.
- Returning the created children and their relationship to the parent.
- Enforcing authorization, ownership, hierarchy, and database consistency.

### Out of scope

- Automatic decomposition by an AI model.
- Assignment of children to users or teams.
- Scheduling, billing, time tracking, notifications, and attachments.
- Editing or deleting a child after creation.
- Decomposing a child in the same request.

## 3. Actors and Invariants

- **Authenticated requester:** may decompose a parent only when authorized by the work-order policy.
- **Parent work order:** must exist, be active, and not itself be a child of another work order.
- **Child work orders:** belong to exactly one parent and inherit the parent's project/tenant boundary.
- **Decomposition request:** contains at least two and at most twenty children.
- **Atomicity:** validation, authorization, and child creation occur in one database transaction.
- **Idempotency:** an idempotency key is scoped to the authenticated user and parent work order.

## 4. UI Contract

### 4.1 Form

The decomposition screen is opened from an existing work-order detail view. It contains a repeatable child-work item row with title, description, estimate, priority, and due date fields. The submit action remains disabled until the form is valid and at least two rows exist.

The UI must not treat client-side validation as an authorization decision. The API response remains authoritative and field errors are mapped back to the corresponding child row.

### 4.2 Validation rules

| Field | Required | Validation | UI error | API error path |
| --- | --- | --- | --- | --- |
| Parent work order ID | Yes | UUID; supplied by route, not editable | `Invalid work order.` | `parent_id` |
| Children | Yes | Array length from 2 through 20 | `Add between 2 and 20 child work orders.` | `children` |
| Child title | Yes | 1-200 characters after trimming; must not be blank | `Enter a title between 1 and 200 characters.` | `children[i].title` |
| Child description | No | Maximum 2,000 characters | `Description must be 2,000 characters or fewer.` | `children[i].description` |
| Estimate minutes | Yes | Integer from 1 through 31,680 | `Enter an estimate from 1 to 31,680 minutes.` | `children[i].estimate_minutes` |
| Priority | Yes | One of `low`, `medium`, `high`, `urgent` | `Select a valid priority.` | `children[i].priority` |
| Due date | No | ISO 8601 date; not before today; not after the parent due date when one exists | `Choose a valid due date within the parent due date.` | `children[i].due_date` |
| Idempotency key | Generated | UUID v4 generated per submit attempt and reused on retry | `Unable to safely retry this request.` | `Idempotency-Key` |

### 4.3 UI states

| State | Behavior |
| --- | --- |
| Loading | Show a non-editable submit state while the request is in flight. Preserve entered values. |
| Success | Navigate to the parent work-order detail view and show the created child count. |
| Validation failure (`422`) | Keep the form open, focus the first invalid field, and show field-level messages from `errors`. |
| Not found (`404`) | Show that the parent no longer exists and provide a link back to the work-order list. |
| Forbidden/conflict (`403`/`409`) | Keep entered data, explain that the parent changed or is not actionable, and do not retry automatically. |
| Network/5xx failure | Keep entered data and offer retry with the same `Idempotency-Key`. |

## 5. Data Model

### 5.1 PII and security classification

Work-order titles and descriptions are **confidential business data** and may contain user-entered personal information. They must be encrypted in transit, protected by tenant and authorization checks, excluded from application logs, and redacted in error telemetry. User IDs and tenant IDs are identifiers, not display data; they must not be accepted from the client when they can be derived from the authenticated session.

### 5.2 PostgreSQL DDL

The following DDL assumes the service owns the `work_orders` table. `gen_random_uuid()` is provided by `pgcrypto`.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE work_order_status AS ENUM (
    'draft',
    'active',
    'completed',
    'cancelled'
);

CREATE TYPE work_order_priority AS ENUM (
    'low',
    'medium',
    'high',
    'urgent'
);

CREATE TABLE work_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    parent_id UUID NULL,
    root_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status work_order_status NOT NULL DEFAULT 'active',
    priority work_order_priority NOT NULL DEFAULT 'medium',
    estimate_minutes INTEGER NOT NULL CHECK (estimate_minutes BETWEEN 1 AND 31680),
    due_date DATE,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT work_orders_parent_fk
        FOREIGN KEY (parent_id) REFERENCES work_orders (id),
    CONSTRAINT work_orders_root_fk
        FOREIGN KEY (root_id) REFERENCES work_orders (id),
    CONSTRAINT work_orders_title_not_blank
        CHECK (length(btrim(title)) BETWEEN 1 AND 200),
    CONSTRAINT work_orders_description_not_blank
        CHECK (description IS NULL OR length(btrim(description)) BETWEEN 1 AND 2000),
    CONSTRAINT work_orders_due_date_check
        CHECK (due_date IS NULL OR due_date >= DATE '2000-01-01')
);

CREATE INDEX work_orders_tenant_parent_idx
    ON work_orders (tenant_id, parent_id);

CREATE INDEX work_orders_tenant_root_idx
    ON work_orders (tenant_id, root_id);

CREATE TABLE work_order_decompositions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    parent_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT work_order_decompositions_parent_fk
        FOREIGN KEY (parent_id) REFERENCES work_orders (id),
    CONSTRAINT work_order_decompositions_unique_request
        UNIQUE (tenant_id, requested_by, parent_id, idempotency_key)
);

CREATE TABLE work_order_decomposition_items (
    decomposition_id UUID NOT NULL,
    child_id UUID NOT NULL,
    position SMALLINT NOT NULL CHECK (position BETWEEN 0 AND 19),
    PRIMARY KEY (decomposition_id, child_id),
    UNIQUE (decomposition_id, position),
    CONSTRAINT decomposition_items_request_fk
        FOREIGN KEY (decomposition_id)
        REFERENCES work_order_decompositions (id)
        ON DELETE CASCADE,
    CONSTRAINT decomposition_items_child_fk
        FOREIGN KEY (child_id) REFERENCES work_orders (id)
);
```

Application transaction requirements:

1. Lock the parent row with `SELECT ... FOR UPDATE`.
2. Verify tenant, requester authorization, parent status, and `parent_id IS NULL`.
3. Insert the decomposition record using the idempotency key. If it already exists, return its stored children.
4. Insert all child rows with the parent's `tenant_id` and `root_id`.
5. Insert decomposition item rows in request order and commit.

The database cannot enforce every cross-row rule, such as a child's due date being no later than the parent's due date. The service must validate those rules while holding the parent lock.

## 6. REST API

### 6.1 Create a decomposition

`POST /v1/work-orders/{parent_id}/decompositions`

Authentication is required. The server derives `tenant_id` and `requested_by` from the access token. The client must send an `Idempotency-Key` header containing a UUID v4. `Content-Type: application/json` is required.

#### Request

```json
{
  "children": [
    {
      "title": "Inspect affected service",
      "description": "Confirm the failure mode and capture relevant evidence.",
      "estimate_minutes": 60,
      "priority": "high",
      "due_date": "2026-10-05"
    },
    {
      "title": "Implement the corrective change",
      "description": null,
      "estimate_minutes": 240,
      "priority": "medium",
      "due_date": null
    }
  ]
}
```

#### Success response: `201 Created`

```json
{
  "decomposition_id": "9f8c2c7a-2fdc-4ac1-9e5d-1e8fd2a8b2a0",
  "parent_id": "0f4d7a31-91c4-4cc0-9a19-8c8f7df3d311",
  "children": [
    {
      "id": "7df9d2b9-6540-43d8-8c2c-f87d6c4aa111",
      "parent_id": "0f4d7a31-91c4-4cc0-9a19-8c8f7df3d311",
      "title": "Inspect affected service",
      "description": "Confirm the failure mode and capture relevant evidence.",
      "status": "active",
      "priority": "high",
      "estimate_minutes": 60,
      "due_date": "2026-10-05",
      "created_at": "2026-09-21T10:15:00Z"
    }
  ],
  "created_at": "2026-09-21T10:15:00Z"
}
```

The response contains every created child; the example shows one item for brevity. The `Location` header points to `/v1/work-orders/{parent_id}/decompositions/{decomposition_id}`.

### 6.2 Retrieve a decomposition

`GET /v1/work-orders/{parent_id}/decompositions/{decomposition_id}`

Returns `200 OK` with the same response shape as creation. This endpoint supports safe client retries and audit/detail views.

### 6.3 Error contract

All errors use `application/problem+json`:

```json
{
  "type": "https://api.example.com/problems/validation-error",
  "title": "Request validation failed",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "errors": [
    {
      "path": "children[1].title",
      "code": "string_too_long",
      "message": "Title must be 200 characters or fewer."
    }
  ],
  "request_id": "req_01J8V3ZQ2N5P7A4C6D8E0F1G2H"
}
```

| Status | Condition | Client behavior |
| --- | --- | --- |
| `201` | All children created | Render the result and parent/child links. |
| `400` | Malformed JSON or missing `Idempotency-Key` | Correct the request; do not retry unchanged. |
| `401` | Missing or invalid authentication | Re-authenticate. |
| `403` | Requester cannot modify the parent | Show an authorization message; do not retry. |
| `404` | Parent or decomposition is not visible to requester | Treat as unavailable without disclosing existence. |
| `409` | Parent is completed/cancelled, is already a child, or the idempotency key was reused with a different body | Refresh state or use a new key only after correcting the request. |
| `422` | Field or business-rule validation failed | Map `errors[].path` to the form. |
| `500` | Unexpected server failure | Retry with the same idempotency key. |

For an idempotent replay, the server returns the original `201` response body and does not create additional children. Reusing an idempotency key with a different normalized request body returns `409` with code `idempotency_key_reused`.

## 7. Acceptance Criteria

- Given an authenticated, authorized user and an active root work order, when a valid request contains two to twenty children, then the API returns `201` and persists all children atomically.
- Given invalid child data, when the request is submitted, then the API returns `422` with one field-level error per invalid path.
- Given an unauthorized or invisible parent, when decomposition is requested, then the API returns `403` or `404` according to the authorization policy without leaking parent data.
- Given a completed, cancelled, or already-child parent, when decomposition is requested, then the API returns `409` and creates no records.
- Given a transient failure followed by a retry using the same idempotency key and body, then exactly one decomposition and its children exist.
- Given a request containing a child due date after the parent's due date, when submitted, then the API returns `422` and creates no children.

## 8. Implementation Boundaries

- API schemas and route handlers belong under `src/modules/work_orders/`.
- Unit tests belong under `tests/unit/` and must cover validation, authorization outcomes, transaction rollback, and idempotent replay.
- No external dependency is required beyond the approved FastAPI/Pydantic/PostgreSQL stack.
- Logs and metrics may include IDs, counts, status codes, and durations, but must not include titles, descriptions, or raw request bodies.