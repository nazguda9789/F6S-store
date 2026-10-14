// staff-chat.js - Quản lý chat cho nhân viên với TinyMCE

let currentUserId = null;
let currentUserName = null;
let currentUserEmail = null;
let staffChatEditor = null;

// Khởi tạo khi trang load
document.addEventListener('DOMContentLoaded', () => {
    loadUserList();
    initWebSocket();
    setupEventListeners();
    initTinyMCE(); // Khởi tạo TinyMCE
});

// ==================== LOAD DANH SÁCH USER ====================
async function loadUserList(search = '') {
    const userListDiv = document.getElementById('staffChatUserList');
    if (!userListDiv) return;

    userListDiv.innerHTML = `
        <div class="text-center py-4 text-muted">
            <i class="fas fa-spinner fa-spin fa-2x mb-2"></i>
            <p>Đang tải danh sách...</p>
        </div>
    `;

    try {
        let url = '/api/chat-realtime/admin/getAllUserChat';
        if (search) {
            url += `?search=${encodeURIComponent(search)}`;
        }

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load users');

        const users = await response.json();
        renderUserList(users);

    } catch (error) {
        console.error('Error loading users:', error);
        userListDiv.innerHTML = `
            <div class="text-center py-4 text-danger">
                <i class="fas fa-exclamation-circle fa-2x mb-2"></i>
                <p>Không thể tải danh sách</p>
                <button class="btn btn-sm btn-outline-danger mt-2" onclick="loadUserList()">
                    <i class="fas fa-redo"></i> Thử lại
                </button>
            </div>
        `;
    }
}

// Render danh sách user
function renderUserList(users) {
    const userListDiv = document.getElementById('staffChatUserList');
    if (!userListDiv) return;

    if (!users || users.length === 0) {
        userListDiv.innerHTML = `
            <div class="text-center py-4 text-muted">
                <i class="fas fa-inbox fa-2x mb-2 opacity-50"></i>
                <p>Chưa có khách hàng nào</p>
            </div>
        `;
        return;
    }

    let html = '';
    users.forEach(user => {
        const displayName = user.fullName || user.email;
        const avatarText = getAvatarText(displayName);
        html += `
            <div class="staff-chat-user-item" data-user-id="${user.id}" 
                 data-user-name="${escapeHtml(displayName)}" 
                 data-user-email="${escapeHtml(user.email)}">
                <div class="staff-chat-avatar">
                    ${avatarText}
                </div>
                <div class="staff-chat-user-info">
                    <div class="staff-chat-user-name">${escapeHtml(displayName)}</div>
                    <div class="staff-chat-user-email">${escapeHtml(user.email)}</div>
                </div>
            </div>
        `;
    });

    userListDiv.innerHTML = html;

    // Thêm click event cho từng user item
    document.querySelectorAll('.staff-chat-user-item').forEach(item => {
        item.addEventListener('click', () => {
            document.querySelectorAll('.staff-chat-user-item').forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            const userId = parseInt(item.dataset.userId);
            const userName = item.dataset.userName;
            const userEmail = item.dataset.userEmail;

            loadChatHistory(userId, userName, userEmail);
        });
    });
}

// ==================== LOAD LỊCH SỬ CHAT ====================
async function loadChatHistory(userId, userName, userEmail) {
    currentUserId = userId;
    currentUserName = userName;
    currentUserEmail = userEmail;

    // Cập nhật header
    const avatarText = document.getElementById('staffChatAvatarText');
    const userNameEl = document.getElementById('staffChatUserName');
    const userEmailEl = document.getElementById('staffChatUserEmail');

    if (avatarText) avatarText.textContent = getAvatarText(userName);
    if (userNameEl) userNameEl.textContent = userName;
    if (userEmailEl) userEmailEl.textContent = userEmail;

    // Hiển thị chat content, ẩn empty state
    const emptyState = document.getElementById('staffChatEmptyState');
    const chatContent = document.getElementById('staffChatContent');

    if (emptyState) emptyState.style.display = 'none';
    if (chatContent) chatContent.style.display = 'flex';

    // Hiển thị loading
    const messagesContainer = document.getElementById('staffChatMessages');
    if (messagesContainer) {
        messagesContainer.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="fas fa-spinner fa-spin fa-2x mb-2"></i>
                <p>Đang tải tin nhắn...</p>
            </div>
        `;
    }

    try {
        const response = await fetch(`/api/chat-realtime/admin/getListChat?idUser=${userId}`);
        if (!response.ok) throw new Error('Failed to load messages');

        const messages = await response.json();
        renderMessages(messages);

    } catch (error) {
        console.error('Error loading messages:', error);
        if (messagesContainer) {
            messagesContainer.innerHTML = `
                <div class="text-center py-4 text-danger">
                    <i class="fas fa-exclamation-circle fa-2x mb-2"></i>
                    <p>Không thể tải tin nhắn</p>
                    <button class="btn btn-sm btn-outline-danger mt-2" onclick="loadChatHistory(${userId}, '${userName}', '${userEmail}')">
                        <i class="fas fa-redo"></i> Thử lại
                    </button>
                </div>
            `;
        }
    }
}

// Render tin nhắn (hiển thị HTML từ TinyMCE)
function renderMessages(messages) {
    const messagesContainer = document.getElementById('staffChatMessages');
    if (!messagesContainer) return;

    if (!messages || messages.length === 0) {
        messagesContainer.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="fas fa-comment-smile fa-2x mb-2 opacity-50"></i>
                <p>Chưa có tin nhắn nào</p>
                <small>Hãy gửi tin nhắn để bắt đầu trò chuyện</small>
            </div>
        `;
        return;
    }

    let html = '';
    messages.forEach(msg => {
        const isUser = msg.type === 'USER';
        const time = formatTime(msg.createdDate);
        html += `
            <div class="staff-chat-message ${isUser ? 'user' : 'staff'}">
                <div>
                    <div class="staff-chat-message-content">
                        ${msg.content}
                    </div>
                    <div class="staff-chat-message-time">${time}</div>
                </div>
            </div>
        `;
    });

    messagesContainer.innerHTML = html;
    scrollToBottom();
}

// ==================== GỬI TIN NHẮN ====================
async function sendStaffMessage() {
    // Lấy nội dung từ TinyMCE
    let message = '';
    let plainText = '';

    if (staffChatEditor) {
        message = staffChatEditor.getContent();
        plainText = staffChatEditor.getContent({ format: 'text' }).trim();
    } else {
        const input = document.getElementById('staffChatEditor');
        if (input) {
            message = input.value.trim();
            plainText = message;
        }
    }

// Kiểm tra: nếu có message (có thể là HTML chứa ảnh) và có currentUserId
    if ((!message || message === '<p><br></p>' || message === '<p></p>') || !currentUserId) return;

// Append tin nhắn tạm thời (hiển thị HTML)
    appendMessageLocally(message, 'STAFF');

// Clear editor
    if (staffChatEditor) {
        staffChatEditor.setContent('');
    } else {
        const input = document.getElementById('staffChatEditor');
        if (input) input.value = '';
    }

    try {
        const response = await fetch('/api/chat-realtime/employee-admin/post', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                content: message,
                user: { id: currentUserId }
            })
        });

        if (!response.ok) throw new Error('Failed to send message');

        const result = await response.json();
        console.log('Message sent:', result);

    } catch (error) {
        console.error('Error sending message:', error);
        appendMessageLocally('Không thể gửi tin nhắn. Vui lòng thử lại!', 'STAFF', true);
    }
}

// Append tin nhắn vào UI (hiển thị HTML)
function appendMessageLocally(message, type, isError = false) {
    const messagesContainer = document.getElementById('staffChatMessages');
    if (!messagesContainer) return;

    const isUser = type === 'USER';
    const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    // Nếu đang ở empty state, xóa nó
    const emptyText = messagesContainer.querySelector('.text-center');
    if (emptyText && messagesContainer.children.length === 1) {
        messagesContainer.innerHTML = '';
    }

    const errorClass = isError ? 'error' : '';
    const messageDiv = document.createElement('div');
    messageDiv.className = `staff-chat-message ${isUser ? 'user' : 'staff'}`;
    messageDiv.innerHTML = `
        <div>
            <div class="staff-chat-message-content ${errorClass}">
                ${isError ? '⚠️ ' : ''}${message}
            </div>
            <div class="staff-chat-message-time">${time}</div>
        </div>
    `;

    messagesContainer.appendChild(messageDiv);
    scrollToBottom();
}

// ==================== WEBSOCKET ====================
function initWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('WebSocket connected:', frame);

        // Subscribe vào topic staff để nhận tin nhắn từ user
        stompClient.subscribe('/topic/chat-by-staff', function(message) {
            const chatting = JSON.parse(message.body);
            // Nếu tin nhắn từ user hiện tại đang chat
            if (chatting.user && chatting.user.id === currentUserId) {
                appendMessageLocally(chatting.content, 'USER');
            }
        });

    }, function(error) {
        console.error('WebSocket connection failed:', error);
        setTimeout(initWebSocket, 5000);
    });
}

// ==================== TINYMCE ====================
// Khởi tạo TinyMCE
function initTinyMCE() {
    tinymce.init({
        selector: '#staffChatEditor',
        // Tăng cường các plugin để hỗ trợ nhiều chức năng hơn
        plugins: 'advlist autolink lists link image charmap print preview hr anchor pagebreak ' +
            'searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking ' +
            'table emoticons template paste codesample help',
        // Định nghĩa các nút trên thanh công cụ. Chia thành nhiều dòng để dễ quản lý.
        toolbar: 'undo redo | blocks | ' + // Hoàn tác, làm lại, và định dạng khối (Heading 1, 2, Paragraph,...)
            'bold italic underline strikethrough | forecolor backcolor | ' + // Định dạng chữ: đậm, nghiêng, gạch chân, gạch ngang, màu chữ, màu nền
            'alignleft aligncenter alignright alignjustify | outdent indent | ' + // Căn lề, lùi dòng
            'numlist bullist | link image media table | ' + // Danh sách số, danh sách dấu chấm, chèn liên kết, ảnh, video, bảng
            'charmap emoticons | fullscreen code preview | help', // Ký tự đặc biệt, biểu tượng cảm xúc, chế độ toàn màn hình, xem trước, mã nguồn, trợ giúp
        height: 100, // Tăng chiều cao của trình soạn thảo
        menubar:false,
        setup: function(editor) {
            staffChatEditor = editor;
            // Xử lý gửi tin nhắn khi nhấn Enter (không phải Shift+Enter)
            editor.on('keydown', function(e) {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    sendStaffMessage();
                }
            });
        },
        // Upload image nếu cần
        images_upload_handler: async function (blobInfo, success, failure) {
            const formData = new FormData();
            formData.append('file', blobInfo.blob(), blobInfo.filename());

            const res = await fetch('/adm-prj/qtht-001/api/upload/upload-file', {
                method: 'POST',
                body: formData
            });
            if (res.status < 300) {
                var linkImage = await res.text();
                success(linkImage);
            }
        }
    });
}

// ==================== UTILITY FUNCTIONS ====================
function scrollToBottom() {
    const container = document.getElementById('staffChatMessages');
    if (container) {
        container.scrollTop = container.scrollHeight;
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

function getAvatarText(name) {
    if (!name) return 'KH';
    const words = name.split(' ');
    if (words.length >= 2) {
        return (words[0][0] + words[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== EVENT LISTENERS ====================
function setupEventListeners() {
    // Tìm kiếm
    const searchInput = document.getElementById('staffChatSearch');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                loadUserList(e.target.value);
            }, 500);
        });
    }

    // Gửi tin nhắn (button click)
    const sendBtn = document.getElementById('staffChatSendBtn');
    if (sendBtn) {
        sendBtn.addEventListener('click', sendStaffMessage);
    }

    // Đóng sidebar trên mobile
    const closeSidebarBtn = document.getElementById('staffChatCloseSidebar');
    if (closeSidebarBtn) {
        closeSidebarBtn.addEventListener('click', () => {
            const sidebar = document.getElementById('staffChatSidebar');
            if (sidebar) sidebar.classList.remove('show');
        });
    }
}

// ==================== EXPORT FUNCTIONS (global) ====================
window.loadUserList = loadUserList;
window.loadChatHistory = loadChatHistory;
window.sendStaffMessage = sendStaffMessage;