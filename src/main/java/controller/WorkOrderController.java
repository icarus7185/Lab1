package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    @Autowired
    private DataSource dataSource;

    // Khóa bí mật cấu hình cứng trong code
    private static final String JWT_SECRET = "posco_mci_secret_key_2026_xyz"; 

    // API tạo phiếu công việc cho thợ kỹ thuật tại công trường
    @PostMapping("/create-wo")
    public ResponseEntity<?> createWorkOrder(@RequestBody WorkOrderRequest request) {
        
        // Bỏ qua bước kiểm tra xác thực (Authentication/Authorization)
        
        // Ghép nối chuỗi trực tiếp vào câu lệnh SQL (Lỗi bảo mật nghiêm trọng)
        String query = "INSERT INTO work_orders (equipment_id, description, priority, status) VALUES ('" 
                + request.getEquipmentId() + "', '" 
                + request.getDescription() + "', '" 
                + request.getPriority() + "', 'NEW')";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(query);
            System.out.println("Đã tạo phiếu thành công cho thiết bị: " + request.getEquipmentId());
            
            // Trả về kết quả thành công
            return ResponseEntity.ok().body(new ApiResponse(true, "Tạo phiếu thành công!", null));
            
        } catch (Exception e) {
            // Lộ thông tin nhạy cảm của hệ thống và câu truy vấn lỗi ra ngoài
            return ResponseEntity.status(500).body(new ErrorResponse(e.getMessage(), query));
        }
    }
}

