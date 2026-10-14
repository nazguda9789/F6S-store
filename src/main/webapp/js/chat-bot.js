// chat-bot.js - Xử lý chat bot với API

// Hàm xử lý khi click button gửi tin nhắn bot
async function handleBotSendClick() {
    const input = document.getElementById('botInput');
    const message = input.value.trim();

    if (message === '') return;

    // Append tin nhắn của người dùng
    appendBotUserMessage(message);

    // Clear input
    input.value = '';

    // Hiển thị typing indicator
    showBotTyping();

    try {
        // Gọi API chat bot
        const response = await callChatBotAPI(message);

        // Ẩn typing indicator
        hideBotTyping();

        // Append tin nhắn từ server
        appendBotServerMessage(response);

    } catch (error) {
        console.error('Error:', error);
        hideBotTyping();
        appendBotServerMessage('Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau!');
    }
}

// Gọi API chat bot
async function callChatBotAPI(message) {
    try {
        const response = await fetch('/api/chat-bot', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message: message })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        return data.reply;

    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    }
}

// Append tin nhắn của người dùng
function appendBotUserMessage(message) {
    const messagesContainer = document.getElementById('botMessages');
    if (!messagesContainer) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message user';

    const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    messageDiv.innerHTML = `
        <div class="message-content">${escapeHtml(message)}</div>
        <div class="message-time">${time}</div>
    `;

    messagesContainer.appendChild(messageDiv);
    scrollToBottom(messagesContainer);
}

// Append tin nhắn từ server/bot
function appendBotServerMessage(message) {
    const messagesContainer = document.getElementById('botMessages');
    if (!messagesContainer) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message bot';

    const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    // Xử lý message có thể là HTML từ Gemini
    messageDiv.innerHTML = `
        <div class="message-content">${formatBotMessage(message)}</div>
        <div class="message-time">${time}</div>
    `;

    messagesContainer.appendChild(messageDiv);
    scrollToBottom(messagesContainer);
}

// Format tin nhắn bot (xử lý markdown đơn giản)
function formatBotMessage(message) {
    let formatted = escapeHtml(message);

    // Xử lý xuống dòng
    formatted = formatted.replace(/\n/g, '<br>');

    // Xử lý in đậm
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Xử lý in nghiêng
    formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // Xử lý bullet points
    formatted = formatted.replace(/^[•\-]\s/gm, '• ');

    return formatted;
}

// Hiển thị typing indicator
function showBotTyping() {
    const messagesContainer = document.getElementById('botMessages');
    if (!messagesContainer) return;

    // Remove existing typing indicator
    hideBotTyping();

    const typingDiv = document.createElement('div');
    typingDiv.className = 'message bot typing';
    typingDiv.id = 'botTypingIndicator';
    typingDiv.innerHTML = `
        <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
        </div>
        <div style="font-size: 11px; margin-top: 5px; color: #999;">Bot đang trả lời...</div>
    `;

    messagesContainer.appendChild(typingDiv);
    scrollToBottom(messagesContainer);
}

// Ẩn typing indicator
function hideBotTyping() {
    const typing = document.getElementById('botTypingIndicator');
    if (typing) typing.remove();
}

// Scroll to bottom của container
function scrollToBottom(element) {
    if (element) {
        element.scrollTop = element.scrollHeight;
    }
}

// Helper escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Khởi tạo event listeners khi trang load
document.addEventListener('DOMContentLoaded', () => {
    const sendBtn = document.getElementById('botSendBtn');
    const input = document.getElementById('botInput');

    if (sendBtn) {
        sendBtn.addEventListener('click', handleBotSendClick);
    }

    if (input) {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleBotSendClick();
            }
        });
    }

    // Xử lý suggestion buttons
    const suggestionBtns = document.querySelectorAll('.suggestion-btn');
    suggestionBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const question = btn.textContent;
            const botInput = document.getElementById('botInput');
            const chatBotBox = document.getElementById('chatBotBox');

            // Chỉ xử lý nếu đang mở chat bot
            if (botInput && chatBotBox && chatBotBox.classList.contains('show')) {
                botInput.value = question;
                handleBotSendClick();
            }
        });
    });

    // Tự động focus vào input khi mở chat bot
    const chatBotBox = document.getElementById('chatBotBox');
    if (chatBotBox) {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                    if (chatBotBox.classList.contains('show')) {
                        setTimeout(() => {
                            const input = document.getElementById('botInput');
                            if (input) input.focus();
                        }, 300);
                    }
                }
            });
        });

        observer.observe(chatBotBox, { attributes: true });
    }
});