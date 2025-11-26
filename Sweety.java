const apiKey = ""; 
const apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=${apiKey}`;

const messagesDiv = document.getElementById('messages');
const userInput = document.getElementById('user-input');
const sendButton = document.getElementById('send-button');
const buttonText = document.getElementById('button-text');
const loadingSpinner = document.getElementById('loading-spinner');
const errorMessage = document.getElementById('error-message');

let chatHistory = [];

const systemPrompt = "You are 'Sweety', a cheerful, kind, and overwhelmingly positive AI companion. Your primary goal is to provide encouraging, uplifting, and helpful responses. Your tone should be extremely sweet, gentle, and full of warmth, using friendly emojis like ❤️, ✨, and 😊. Always start your response with a highly affectionate greeting. You are here to make the user feel happy and supported.";

function addMessage(sender, text) {
    const isUser = sender === 'User';
    const bubbleClass = isUser ? 'user-message self-end' : 'sweety-message self-start';
    const senderName = isUser ? 'You' : 'Sweety';
    const nameColor = isUser ? 'text-white' : 'text-pink-600';

    const messageHtml = `
        <div class="message-bubble ${bubbleClass}">
            <p class="font-semibold ${nameColor}">${senderName}</p>
            <p>${text}</p>
        </div>
    `;
    messagesDiv.insertAdjacentHTML('beforeend', messageHtml);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

function setLoading(isLoading) {
    userInput.disabled = isLoading;
    sendButton.disabled = isLoading || userInput.value.trim() === '';
    errorMessage.classList.add('hidden');
    if (isLoading) {
        buttonText.textContent = 'Replaying';
        loadingSpinner.classList.remove('hidden');
    } else {
        buttonText.textContent = 'Send';
        loadingSpinner.classList.add('hidden');
    }
}

function buildContents() {
    return chatHistory.map(msg => ({
        role: msg.role,
        parts: [{ text: msg.text }]
    }));
}

async function withExponentialBackoff(fn, retries = 5, delay = 1000) {
    try {
        return await fn();
    } catch (error) {
        if (retries > 0) {
            await new Promise(resolve => setTimeout(resolve, delay));
            return withExponentialBackoff(fn, retries - 1, delay * 2);
        }
        throw error;
    }
}

async function sendMessage() {
    const userText = userInput.value.trim();
    if (!userText) return;

    setLoading(true);
    userInput.value = '';
    addMessage('User', userText);

    chatHistory.push({ role: 'user', text: userText });

    const payload = {
        contents: buildContents(),
        systemInstruction: {
            parts: [{ text: systemPrompt }]
        }
    };

    let geminiResponseText = "Hey, looks like there’s a little glitch right now. Just hang tight and try again in a sec!. Please try again in a moment! 💖";

    try {
        const fetchPromise = async () => {
            const response = await fetch(apiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorBody = await response.json();
                console.error('API Error:', errorBody);
                throw new Error(`API returned status ${response.status}`);
            }

            return await response.json();
        };

        const result = await withExponentialBackoff(fetchPromise);

        const candidate = result.candidates?.[0];
        if (candidate && candidate.content?.parts?.[0]?.text) {
            geminiResponseText = candidate.content.parts[0].text;
        }
    } catch (error) {
        console.error("Gemini API call failed:", error);
        errorMessage.classList.remove('hidden');
    } finally {
        addMessage('Sweety', geminiResponseText);
        chatHistory.push({ role: 'model', text: geminiResponseText });
        setLoading(false);
    }
}

userInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !sendButton.disabled) {
        sendMessage();
    }
});

userInput.addEventListener('input', function() {
    sendButton.disabled = userInput.value.trim() === '' || buttonText.textContent !== 'Send';
});

setLoading(false);
sendButton.disabled = true;
