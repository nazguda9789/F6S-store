// chat-realtime.js - Xử lý chat với nhân viên

// Hàm xử lý khi click button gửi tin nhắn
async function handleStaffSendClick() {
    const input = document.getElementById('staffInput');
    const message = input.value.trim();

    if (message === '') return;
    var payload = {
        "content":message,
        "createdDate":null
    }
    appendStaffUserMessage(payload);

    // Clear input
    input.value = '';
    try {
        // showStaffTyping();
        await sendMessageToServer(message);
    } catch (error) {
        console.error('Error:', error);
        hideStaffTyping();
        appendStaffServerMessage('Xin lỗi, không thể gửi tin nhắn. Vui lòng thử lại sau!', true);
    }
}

// Append tin nhắn của người dùng
function appendStaffUserMessage(message) {
    const messagesContainer = document.getElementById('staffMessages');
    if (!messagesContainer) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message user';

    const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    messageDiv.innerHTML = `
        <div class="message-content">${escapeHtml(message.content)}</div>
        <div class="message-time">${message.createdDate == null?time:formatTime(message.createdDate)}</div>
    `;

    messagesContainer.appendChild(messageDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// Append tin nhắn từ server (nhân viên)
function appendStaffServerMessage(message, isError = false) {
    const messagesContainer = document.getElementById('staffMessages');
    if (!messagesContainer) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = `message staff ${isError ? 'error' : ''}`;

    const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    messageDiv.innerHTML = `
        <div class="message-content">
            ${isError ? '⚠️ ' : ''}${message.content}
        </div>
        <div class="message-time">${formatTime(message.createdDate)}</div>
    `;

    messagesContainer.appendChild(messageDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// Hiển thị typing indicator (nhân viên đang nhập)
function showStaffTyping() {
    const messagesContainer = document.getElementById('staffMessages');
    if (!messagesContainer) return;

    // Remove existing typing indicator
    hideStaffTyping();

    const typingDiv = document.createElement('div');
    typingDiv.className = 'message staff typing';
    typingDiv.id = 'staffTypingIndicator';
    typingDiv.innerHTML = `
        <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
        </div>
        <div style="font-size: 11px; margin-top: 5px;">Nhân viên đang nhập...</div>
    `;

    messagesContainer.appendChild(typingDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// Ẩn typing indicator
function hideStaffTyping() {
    const typing = document.getElementById('staffTypingIndicator');
    if (typing) typing.remove();
}

async function sendMessageToServer(message) {
    var payload = {
        "content":message
    }
    const response = await fetch('/api/chat-realtime/user/post', {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(payload)
    });
    if (response.status > 300) {
        appendStaffServerMessage('Không thể kết nối đến server. Vui lòng kiểm tra kết nối!', true);
    }
}

// Hàm xử lý khi nhận tin nhắn từ server (WebSocket)
function onStaffMessageReceived(message) {
    hideStaffTyping();
    appendStaffServerMessage(message);
}

// Helper escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Khởi tạo event listeners khi trang load
document.addEventListener('DOMContentLoaded', () => {
    const sendBtn = document.getElementById('staffSendBtn');
    const input = document.getElementById('staffInput');

    if (sendBtn) {
        sendBtn.addEventListener('click', handleStaffSendClick);
    }

    if (input) {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleStaffSendClick();
            }
        });
    }

    // Khởi tạo WebSocket nếu cần
    initStaffWebSocket();
    loadStaffMessages();
});

// Khởi tạo WebSocket cho chat realtime
function initStaffWebSocket() {
    const staffOption = document.getElementById('chatStaffOption');
    if (!staffOption) return;

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/chat-by-user/'+document.getElementById("userIdChat").value, function (message) {
            const receivedMessage = JSON.parse(message.body);
            // Xử lý hiển thị tin nhắn
            appendStaffServerMessage(receivedMessage, false);
        });
    }, function (error) {
        console.error('Connection socket failed:', error);
        setTimeout(connect, 5000);
    });
}

// Hàm load tin nhắn từ server
async function loadStaffMessages() {
    try {
        const response = await fetch('/api/chat-realtime/user/my-chat');
        if (response.ok) {
            const messages = await response.json();
            const messagesContainer = document.getElementById('staffMessages');
            if (messagesContainer) {
                // Xóa tin nhắn chào mặc định
                const defaultMessage = messagesContainer.querySelector('.message.staff');
                if (defaultMessage && messages.length > 0) {
                    defaultMessage.remove();
                }
                // Hiển thị tin nhắn cũ
                messages.forEach(msg => {
                    if (msg.type === 'USER') {
                        appendStaffUserMessage(msg);
                    } else {
                        appendStaffServerMessage(msg, false);
                    }
                });
                // Cuộn xuống tin nhắn cuối
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            }
        }
    } catch (error) {
        console.error('Error loading messages:', error);
    }
}

function formatTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    // Dữ liệu từ server có dạng "HH:mm dd/MM/yyyy"
    // Ví dụ: "14:30 25/12/2024"
    if (typeof dateTimeStr === 'string') {
        // Lấy phần giờ phút (2 ký tự đầu)
        const timePart = dateTimeStr.split(' ')[0];
        return timePart || '';
    }
    return '';
}