// chat.js - Quản lý state và UI chung

// State variables
let currentChat = null;
let isOpen = false;

// DOM Elements
let chatButton, chatOptions, chatBotBox, chatStaffBox, botOption, staffOption;

// Initialize chat when DOM is ready
document.addEventListener('DOMContentLoaded', async () => {
    await initChat();
});

// Main initialization function
async function initChat() {
    console.log('Initializing chat system...');

    // Get DOM elements
    chatButton = document.getElementById('chatButton');
    chatOptions = document.getElementById('chatOptions');
    chatBotBox = document.getElementById('chatBotBox');
    chatStaffBox = document.getElementById('chatStaffBox');
    botOption = document.getElementById('chatBotOption');
    staffOption = document.getElementById('chatStaffOption');

    if (!chatButton) {
        console.error('Chat button not found!');
        return;
    }

    // Setup event listeners
    await setupEventListeners();
    console.log('Chat system initialized successfully');
}

// Setup all event listeners
async function setupEventListeners() {
    // Chat button click
    chatButton.addEventListener('click', handleChatButtonClick);

    // Close options when clicking outside
    document.addEventListener('click', handleOutsideClick);

    // Bot option click
    if (botOption) {
        botOption.addEventListener('click', handleBotOptionClick);
    }

    // Staff option click
    if (staffOption) {
        staffOption.addEventListener('click', handleStaffOptionClick);
    }

    // Close buttons for chat boxes
    const closeButtons = document.querySelectorAll('.close-chat');
    closeButtons.forEach(btn => {
        btn.addEventListener('click', handleCloseChat);
    });
}

// Handle chat button click
async function handleChatButtonClick(event) {
    event.stopPropagation();
    console.log('Chat button clicked, currentChat:', currentChat);

    if (currentChat) {
        await closeAll();
    } else {
        toggleOptions();
    }
}

// Toggle options menu
function toggleOptions() {
    chatOptions.classList.toggle('show');
    isOpen = chatOptions.classList.contains('show');
}

// Handle click outside
async function handleOutsideClick(event) {
    // Close options if clicking outside
    if (chatOptions && chatOptions.classList.contains('show') &&
        !chatButton.contains(event.target) && !chatOptions.contains(event.target)) {
        chatOptions.classList.remove('show');
        isOpen = false;
    }

    // Close chat if clicking outside
    if (currentChat) {
        const currentBox = currentChat === 'bot' ? chatBotBox : chatStaffBox;
        if (currentBox && !currentBox.contains(event.target) && !chatButton.contains(event.target)) {
            await closeAll();
        }
    }
}

// Handle bot option click
async function handleBotOptionClick(event) {
    if (event) event.stopPropagation();
    console.log('Opening chat bot');
    await openChatBot();
}

// Handle staff option click
async function handleStaffOptionClick(event) {
    if (event) event.stopPropagation();
    console.log('Opening chat staff');
    await openChatStaff();
}

// Open chat bot
async function openChatBot() {
    await closeAll();
    chatBotBox.classList.add('show');
    currentChat = 'bot';

    // Dispatch event for chat bot
    const event = new CustomEvent('chatOpened', { detail: { type: 'bot' } });
    document.dispatchEvent(event);
}

// Open chat staff
async function openChatStaff() {
    await closeAll();
    chatStaffBox.classList.add('show');
    currentChat = 'staff';

    // Dispatch event for chat staff
    const event = new CustomEvent('chatOpened', { detail: { type: 'staff' } });
    document.dispatchEvent(event);
}

// Close all chats
async function closeAll() {
    console.log('Closing all chats');
    if (chatOptions) chatOptions.classList.remove('show');
    if (chatBotBox) chatBotBox.classList.remove('show');
    if (chatStaffBox) chatStaffBox.classList.remove('show');
    currentChat = null;
    isOpen = false;
}

// Get current chat type
function getCurrentChat() {
    return currentChat;
}

// Check if chat is open
function isChatOpen() {
    return currentChat !== null;
}

// Hàm đóng chat box
function handleCloseChat(event) {
    // Dừng sự kiện lan truyền
    event.stopPropagation();

    // Lấy chat box chứa nút close
    const closeBtn = event.currentTarget;
    const chatBox = closeBtn.closest('.chat-box');

    if (chatBox) {
        // Ẩn chat box
        chatBox.classList.remove('show');

        // Nếu đang có currentChat, reset nó
        if (window.currentChat === 'bot' && chatBox.id === 'chatBotBox') {
            window.currentChat = null;
        } else if (window.currentChat === 'staff' && chatBox.id === 'chatStaffBox') {
            window.currentChat = null;
        }

        // Optional: Clear input khi đóng chat
        const input = chatBox.querySelector('input[type="text"], textarea');
        if (input) {
            input.value = '';
        }

        // Optional: Clear TinyMCE editor nếu có
        if (chatBox.id === 'chatStaffBox' && window.staffChatEditor) {
            window.staffChatEditor.setContent('');
        }

        // Optional: Nếu là chat bot, clear typing indicator
        if (chatBox.id === 'chatBotBox') {
            const typing = document.getElementById('botTypingIndicator');
            if (typing) typing.remove();
        }

        // Optional: Nếu là chat staff, clear typing indicator
        if (chatBox.id === 'chatStaffBox') {
            const typing = document.getElementById('staffTypingIndicator');
            if (typing) typing.remove();
        }

        console.log(`Closed chat: ${chatBox.id}`);
    }
}