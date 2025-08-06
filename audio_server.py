#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import socket
import threading
import base64
import json
import time
import wave
import io
import speech_recognition as sr

class AudioProcessingServer:
    def __init__(self, host='0.0.0.0', port=8080):
        self.host = host
        self.port = port
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.clients = []
        
    def start(self):
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(5)
        print(f"🎧 Server đang lắng nghe tại {self.host}:{self.port}")
        print("📱 Chờ kết nối từ Android app...")
        
        while True:
            client_socket, address = self.server_socket.accept()
            print(f"✅ Kết nối mới từ {address}")
            self.clients.append(client_socket)
            client_thread = threading.Thread(target=self.handle_client, args=(client_socket, address))
            client_thread.start()
    
    def handle_client(self, client_socket, address):
        try:
            while True:
                data = client_socket.recv(4096).decode('utf-8')
                if not data:
                    break
                    
                print(f"📨 Nhận từ {address}: {data[:100]}...")
                
                if data.startswith("AUDIO:"):
                    # Xử lý audio data
                    audio_base64 = data[6:]  # Bỏ "AUDIO:"
                    try:
                        audio_data = base64.b64decode(audio_base64)
                        result = self.process_audio(audio_data)
                        
                        if result.startswith("ERROR|"):
                            response = f"ERROR:{result[6:]}"
                            print(f"❌ Lỗi xử lý audio: {result[6:]}")
                        else:
                            response = f"AUDIO_RESULT:{result}"
                            print(f"🎵 Xử lý audio thành công: {result}")
                            
                    except Exception as e:
                        response = f"ERROR:Không thể xử lý audio: {str(e)}"
                        print(f"❌ Lỗi xử lý audio: {e}")
                    
                elif data.startswith("TEXT:"):
                    # Xử lý text command
                    text_command = data[5:]  # Bỏ "TEXT:"
                    processed_command = self.process_text(text_command)
                    response = f"COMMAND:{processed_command}"
                    print(f"📝 Xử lý text: '{text_command}' -> '{processed_command}'")
                    
                else:
                    response = f"ERROR:Unknown command format: {data[:50]}"
                    print(f"❌ Format không hợp lệ: {data[:50]}")
                
                client_socket.send(response.encode('utf-8'))
                print(f"📤 Gửi response: {response}")
                
        except Exception as e:
            print(f"❌ Lỗi xử lý client {address}: {e}")
            error_response = f"ERROR:Server error: {str(e)}"
            try:
                client_socket.send(error_response.encode('utf-8'))
            except:
                pass
        finally:
            if client_socket in self.clients:
                self.clients.remove(client_socket)
            client_socket.close()
            print(f"🔌 Đóng kết nối từ {address}")
    
    def process_audio(self, audio_data):
        """
        Xử lý audio WAV data và trả về screen name + message
        """
        try:
            # Tạo WAV file từ audio data
            wav_file = io.BytesIO(audio_data)
            
            # Sử dụng speech recognition để chuyển audio thành text
            recognizer = sr.Recognizer()
            
            with sr.AudioFile(wav_file) as source:
                audio = recognizer.record(source)
                
                # Thử nhận dạng tiếng Việt trước
                try:
                    text = recognizer.recognize_google(audio, language='vi-VN')
                    print(f"🎤 Nhận dạng tiếng Việt: {text}")
                except:
                    # Fallback về tiếng Anh
                    try:
                        text = recognizer.recognize_google(audio, language='en-US')
                        print(f"🎤 Nhận dạng tiếng Anh: {text}")
                    except:
                        # Nếu không nhận dạng được, trả về lỗi
                        return "ERROR|Không thể nhận dạng giọng nói"
            
            # Xử lý text và trả về kết quả
            result = self.process_text(text)
            
            # Mapping từ command sang screen name
            screen_mapping = {
                "căn cước công dân": "can_cuoc",
                "khai báo tạm trú": "tam_tru", 
                "xin giấy xác nhận cư trú": "xac_nhan_cu_tru",
                "test tts": "test_tts",
                "menu chức năng": "menu"
            }
            
            screen_name = screen_mapping.get(result, "unknown")
            message = f"Bạn đã nói: '{text}'. Chuyển đến màn hình: {result}"
            
            return f"{screen_name}|{message}"
            
        except Exception as e:
            print(f"❌ Lỗi xử lý audio: {e}")
            return f"ERROR|Lỗi xử lý audio: {str(e)}"
    
    def process_text(self, text_command):
        """
        Xử lý text command và trả về kết quả
        Có thể tích hợp với NLP, AI model, etc.
        """
        text = text_command.lower().strip()
        
        # Mapping các từ khóa
        keyword_mapping = {
            # Căn cước công dân
            "căn cước": "căn cước công dân",
            "can cuoc": "căn cước công dân", 
            "cccd": "căn cước công dân",
            "chứng minh": "căn cước công dân",
            "chung minh": "căn cước công dân",
            
            # Tạm trú
            "tạm trú": "khai báo tạm trú",
            "tam tru": "khai báo tạm trú",
            "khai báo": "khai báo tạm trú",
            "tạm vắng": "khai báo tạm trú",
            "tam vang": "khai báo tạm trú",
            
            # Xác nhận cư trú
            "xác nhận": "xin giấy xác nhận cư trú",
            "xac nhan": "xin giấy xác nhận cư trú",
            "cư trú": "xin giấy xác nhận cư trú",
            "cu tru": "xin giấy xác nhận cư trú",
            "giấy xác nhận": "xin giấy xác nhận cư trú",
            
            # Test TTS
            "test": "test tts",
            "tts": "test tts",
            "phát âm": "test tts",
            "phat am": "test tts",
            "đọc text": "test tts",
            "doc text": "test tts",
            "nói": "test tts",
            "noi": "test tts",
            
            # Menu
            "menu": "menu chức năng",
            "chức năng": "menu chức năng",
            "chuc nang": "menu chức năng",
            "danh sách": "menu chức năng",
            "danh sach": "menu chức năng",
            "tất cả": "menu chức năng",
            "tat ca": "menu chức năng"
        }
        
        # Tìm keyword phù hợp nhất
        for keyword, result in keyword_mapping.items():
            if keyword in text:
                return result
        
        # Nếu không tìm thấy, trả về text gốc
        return text
    
    def stop(self):
        """Dừng server"""
        print("🛑 Dừng server...")
        for client in self.clients:
            try:
                client.close()
            except:
                pass
        self.server_socket.close()

if __name__ == "__main__":
    try:
        server = AudioProcessingServer()
        print("🚀 Khởi động Audio Processing Server...")
        print("💡 Nhấn Ctrl+C để dừng server")
        server.start()
    except KeyboardInterrupt:
        print("\n👋 Tạm biệt!")
    except Exception as e:
        print(f"❌ Lỗi khởi động server: {e}") 