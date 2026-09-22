package controller;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workorders")
public class ScratchHandler {

	@PostMapping
	public ResponseEntity<CreateWorkOrderRequest> createWorkOrder(
			@Valid @RequestBody CreateWorkOrderRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidationError(MethodArgumentNotValidException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "The request contains invalid fields.");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleMalformedRequest(HttpMessageNotReadableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "The request body is malformed.");
		return ResponseEntity.badRequest().body(problem);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CreateWorkOrderRequest(
			@NotBlank
			@Size(max = 200)
			String title,
			@Size(max = 2000)
			String description,
			@NotNull
			@Min(1)
			@Max(31680)
			@JsonProperty("estimate_minutes")
			Integer estimateMinutes,
			@NotNull
			@Pattern(regexp = "low|medium|high|urgent")
			String priority,
			@JsonProperty("due_date")
			LocalDate dueDate) {
	}
}
