# Hướng dẫn Setup Server Xử lý Audio qua Socket

## Tổng quan
Ứng dụng Android sẽ kết nối với server qua socket để gửi audio/text và nhận lại kết quả xử lý từ server.

## Protocol giao tiếp

### Client gửi đến Server:
1. **AUDIO:base64_data** - Gửi audio data dưới dạng base64
2. **TEXT:command_text** - Gửi text command để xử lý

### Server trả về Client:
1. **COMMAND:processed_command** - Kết quả xử lý text command
2. **AUDIO_RESULT:screen_name|message** - Kết quả xử lý audio (tên màn hình + message)
3. **ERROR:error_message** - Thông báo lỗi

## Cấu hình Server

### Thay đổi IP và Port trong SocketManager.java:
```java
private static final String SERVER_HOST = "192.168.1.100"; // Thay đổi IP server của bạn
private static final int SERVER_PORT = 8080; // Thay đổi port server của bạn
```

## Ví dụ Server Python

```python
import socket
import threading
import base64
import speech_recognition as sr
from io import BytesIO

class AudioProcessingServer:
    def __init__(self, host='0.0.0.0', port=8080):
        self.host = host
        self.port = port
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
    def start(self):
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(5)
        print(f"Server đang lắng nghe tại {self.host}:{self.port}")
        
        while True:
            client_socket, address = self.server_socket.accept()
            print(f"Kết nối từ {address}")
            client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
            client_thread.start()
    
    def handle_client(self, client_socket):
        try:
            while True:
                data = client_socket.recv(4096).decode('utf-8')
                if not data:
                    break
                    
                if data.startswith("AUDIO:"):
                    # Xử lý audio data
                    audio_base64 = data[6:]  # Bỏ "AUDIO:"
                    audio_data = base64.b64decode(audio_base64)
                    command = self.process_audio(audio_data)
                    response = f"COMMAND:{command}"
                    
                elif data.startswith("TEXT:"):
                    # Xử lý text command
                    text_command = data[5:]  # Bỏ "TEXT:"
                    processed_command = self.process_text(text_command)
                    response = f"COMMAND:{processed_command}"
                    
                else:
                    response = f"ERROR:Unknown command format"
                
                client_socket.send(response.encode('utf-8'))
                
        except Exception as e:
            error_response = f"ERROR:{str(e)}"
            client_socket.send(error_response.encode('utf-8'))
        finally:
            client_socket.close()
    
    def process_audio(self, audio_data):
        # Xử lý audio data (có thể sử dụng speech recognition)
        # Trả về command đã xử lý
        return "processed_audio_command"
    
    def process_text(self, text_command):
        # Xử lý text command và trả về kết quả
        # Có thể tích hợp NLP, AI model, etc.
        return text_command.lower()

if __name__ == "__main__":
    server = AudioProcessingServer()
    server.start()
```

## Cài đặt dependencies cho Python server:
```bash
pip install speech_recognition
pip install pyaudio
pip install wave
```

## Tính năng mới:

### **Ghi âm liên tục:**
- App ghi âm liên tục khi nhấn nút mic
- Tự động phát hiện im lặng 3 giây
- Gửi file WAV đến server để xử lý

### **Speech Recognition:**
- Server sử dụng Google Speech-to-Text API
- Hỗ trợ tiếng Việt và tiếng Anh
- Trả về tên màn hình + message

### **Response format mới:**
```
AUDIO_RESULT:can_cuoc|Bạn đã nói: 'căn cước công dân'. Chuyển đến màn hình: căn cước công dân
```

## Lưu ý quan trọng:

1. **Network Permission**: Đảm bảo app có quyền truy cập internet
2. **Firewall**: Mở port 8080 trên server
3. **IP Address**: Sử dụng IP local hoặc public tùy theo môi trường
4. **Error Handling**: App có fallback về xử lý local nếu server lỗi
5. **Security**: Có thể thêm authentication nếu cần

## Testing:
1. Chạy Python server
2. Chạy Android app
3. Nhấn nút voice và nói lệnh
4. Kiểm tra log để xem kết quả xử lý 