const PIECE_SYMBOLS = {
    KING: '帅',
    ROOK: '车',
    KNIGHT: '马',
    CANNON: '炮',
    PAWN: '兵',
    GUARD: '仕',
    BISHOP: '相'
};

const BLACK_SYMBOLS = {
    KING: '将',
    ROOK: '车',
    KNIGHT: '马',
    CANNON: '炮',
    PAWN: '卒',
    GUARD: '士',
    BISHOP: '象'
};

let socket = null;
let selectedCell = null;
let boardState = {};
let myColor = null;
let currentTurn = null;
let gameStatus = 'waiting';
let opponentReady = false;
let myReady = false;
let timerInterval = null;
let deadlineTime = null;
let heartbeatInterval = null;
let loggedInUser = null;
let roomId = null;

const loginScreen = document.getElementById('loginScreen');
const gameScreen = document.getElementById('gameScreen');
const board = document.getElementById('board');
const statusLine = document.getElementById('gameStatus');
const currentTurnEl = document.getElementById('currentTurn');
const yourColorEl = document.getElementById('yourColor');
const timeLeftEl = document.getElementById('timeLeft');
const messageEl = document.getElementById('message');
const moveLogEl = document.getElementById('moveLog');
const redPlayerEl = document.getElementById('redPlayer');
const blackPlayerEl = document.getElementById('blackPlayer');

document.getElementById('tabLogin').addEventListener('click', () => {
    document.getElementById('tabLogin').classList.add('active');
    document.getElementById('tabRegister').classList.remove('active');
    document.getElementById('authButton').textContent = '登录';
    document.getElementById('nickName').style.display = 'none';
});

document.getElementById('tabRegister').addEventListener('click', () => {
    document.getElementById('tabRegister').classList.add('active');
    document.getElementById('tabLogin').classList.remove('active');
    document.getElementById('authButton').textContent = '注册';
    document.getElementById('nickName').style.display = 'block';
});

document.getElementById('authButton').addEventListener('click', () => {
    const userId = document.getElementById('userId').value.trim();
    const passWord = document.getElementById('passWord').value;
    const nickName = document.getElementById('nickName').value.trim();

    if (!userId || !passWord) {
        showLoginStatus('请填写用户名和密码', 'error');
        return;
    }

    const isRegister = document.getElementById('tabRegister').classList.contains('active');
    if (isRegister && !nickName) {
        showLoginStatus('请填写昵称', 'error');
        return;
    }

    connectAndAuth(userId, passWord, nickName, isRegister);
});

document.getElementById('connectButton').addEventListener('click', () => {
    connectWebSocket();
});

document.getElementById('startMatchButton').addEventListener('click', () => {
    send({ messageType: 'startMatch' });
    statusLine.textContent = '匹配中...';
    setButtonStates({ startMatch: false, cancelMatch: true });
});

document.getElementById('cancelMatchButton').addEventListener('click', () => {
    send({ messageType: 'cancelMatch' });
    statusLine.textContent = '已取消匹配';
    setButtonStates({ startMatch: true, cancelMatch: false });
});

document.getElementById('readyButton').addEventListener('click', () => {
    send({ messageType: 'Ready' });
    myReady = true;
    document.getElementById('readyButton').disabled = true;
    document.getElementById('readyButton').textContent = '已准备';
    messageEl.textContent = '等待对手准备...';
});

document.getElementById('resignButton').addEventListener('click', () => {
    if (confirm('确定要认输吗？')) {
        send({ messageType: 'Resign' });
    }
});

document.getElementById('logoutButton').addEventListener('click', () => {
    logout();
});

function connectAndAuth(userId, passWord, nickName, isRegister) {
    connectWebSocket(() => {
        const message = isRegister
            ? { messageType: 'register', userId, passWord, nickName }
            : { messageType: 'Login', userId, passWord };
        send(message);
    });
}

function connectWebSocket(onOpenCallback) {
    const wsUrl = document.getElementById('wsUrl').value;

    if (socket) {
        socket.close();
    }

    socket = new WebSocket(wsUrl);
    socket.addEventListener('open', () => {
        showLoginStatus('已连接到服务器', 'success');
        if (onOpenCallback) {
            onOpenCallback();
        }
    });

    socket.addEventListener('message', (event) => {
        try {
            const data = JSON.parse(event.data);
            handleMessage(data);
        } catch (e) {
            console.error('Invalid JSON:', event.data);
        }
    });

    socket.addEventListener('close', () => {
        showLoginStatus('连接已断开', 'error');
        clearTimers();
        setButtonStates({ startMatch: false, cancelMatch: false, ready: false, resign: false });
    });

    socket.addEventListener('error', () => {
        showLoginStatus('连接失败', 'error');
    });
}

function send(payload) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        showLoginStatus('连接未建立', 'error');
        return;
    }
    socket.send(JSON.stringify(payload));
}

function handleMessage(data) {
    switch (data.messageType) {
        case 'loginResult':
            handleLoginResult(data);
            break;
        case 'matchSuccess':
            handleMatchSuccess(data);
            break;
        case 'roomInfo':
            handleRoomInfo(data);
            break;
        case 'gameStart':
            handleGameStart(data);
            break;
        case 'moveResult':
            handleMoveResult(data);
            break;
        case 'timeout':
            handleTimeout(data);
            break;
        case 'gameOver':
            handleGameOver(data);
            break;
        case 'pong':
            break;
        case 'error':
            handleError(data);
            break;
        default:
            console.log('Unknown message:', data);
    }
}

function handleLoginResult(data) {
    if (data.success) {
        loggedInUser = { userId: data.userId, nickName: data.nickName };
        showLoginStatus(`登录成功！欢迎, ${data.nickName}`, 'success');
        setTimeout(() => {
            loginScreen.classList.remove('active');
            gameScreen.classList.add('active');
            startHeartbeat();
            setButtonStates({ startMatch: true });
        }, 1000);
    } else {
        showLoginStatus(data.message || '登录失败', 'error');
    }
}

function handleMatchSuccess(data) {
    roomId = data.roomId;
    const opponentName = data.opponentNickName;

    statusLine.textContent = '匹配成功！';
    currentTurnEl.textContent = '等待双方准备';
    messageEl.textContent = `对手: ${opponentName}，请点击"准备"`;

    if (myColor === 'red') {
        redPlayerEl.textContent = `红方: ${loggedInUser.nickName}`;
        blackPlayerEl.textContent = `黑方: ${opponentName}`;
    } else {
        redPlayerEl.textContent = `红方: ${opponentName}`;
        blackPlayerEl.textContent = `黑方: ${loggedInUser.nickName}`;
    }

    gameStatus = 'preparing';
    setButtonStates({ startMatch: false, cancelMatch: false, ready: true });
}

function handleRoomInfo(data) {
    opponentReady = true;
    messageEl.textContent = '对手已准备，请点击"准备"';
}

function handleGameStart(data) {
    myColor = data.yourColor;
    currentTurn = data.firstHand ? 'red' : 'black';
    gameStatus = 'playing';

    yourColorEl.textContent = `你的颜色: ${myColor === 'red' ? '红方' : '黑方'}`;
    statusLine.textContent = '游戏开始！';
    currentTurnEl.textContent = `当前回合: ${currentTurn === 'red' ? '红方' : '黑方'}`;
    messageEl.textContent = currentTurn === myColor ? '轮到你走棋' : '等待对手走棋';

    setButtonStates({ ready: false, resign: true });

    updateTurnIndicator();
    renderBoard(data.initialBoard);
    startTimer();
}

function handleMoveResult(data) {
    if (!data.success) {
        messageEl.textContent = `错误: ${data.message || '走子无效'}`;
        setTimeout(() => {
            if (gameStatus === 'playing') {
                messageEl.textContent = currentTurn === myColor ? '轮到你走棋' : '等待对手走棋';
            }
        }, 2000);
        return;
    }

    const move = data.move;
    const from = `${move.fromX}${move.fromY}`;
    const to = `${move.toX}${move.toY}`;

    updateBoardState(from, to, data.flipResult);

    addMoveLog(move, data.flipResult, data.capturedPiece);

    if (data.capturedPiece) {
        delete boardState[to];
    }

    if (data.currentTurn) {
        currentTurn = data.currentTurn.toLowerCase();
        currentTurnEl.textContent = `当前回合: ${currentTurn === 'red' ? '红方' : '黑方'}`;
        messageEl.textContent = currentTurn === myColor ? '轮到你走棋' : '等待对手走棋';
        updateTurnIndicator();
        startTimer();
    }

    renderBoardFromState();
}

function handleTimeout(data) {
    statusLine.textContent = '超时！';
    messageEl.textContent = data.loserId === loggedInUser.userId ? '你超时了，游戏结束' : '对手超时，你获胜！';
    gameStatus = 'ended';
    clearTimers();
    setButtonStates({ resign: false });
}

function handleGameOver(data) {
    statusLine.textContent = '游戏结束';

    if (!data.winner) {
        messageEl.textContent = '和棋！';
    } else if (data.winnerId === loggedInUser.userId) {
        messageEl.textContent = '恭喜你获胜！';
    } else {
        messageEl.textContent = '你输了';
    }

    gameStatus = 'ended';
    clearTimers();
    setButtonStates({ resign: false });
}

function handleError(data) {
    console.error('Error:', data);
    messageEl.textContent = `错误: ${data.message}`;
}

function renderBoard(initialBoard) {
    board.replaceChildren();
    boardState = {};

    for (let y = 9; y >= 0; y--) {
        for (let col = 0; col < 9; col++) {
            const x = String.fromCharCode('a'.charCodeAt(0) + col);
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.dataset.x = x;
            cell.dataset.y = String(y);
            cell.dataset.coord = `${x}${y}`;
            cell.addEventListener('click', () => handleCellClick(cell));
            board.appendChild(cell);
        }
    }

    initialBoard.forEach(piece => {
        const key = `${piece.x}${piece.y}`;
        boardState[key] = {
            color: piece.color.toLowerCase(),
            type: piece.piece,
            visible: piece.visible
        };
    });

    const boardContainer = document.querySelector('.board-container');
    if (myColor === 'black') {
        board.classList.add('flip-180');
        boardContainer.classList.add('flip-180');
    } else {
        board.classList.remove('flip-180');
        boardContainer.classList.remove('flip-180');
    }

    renderBoardFromState();
}

function renderBoardFromState() {
    const cells = board.querySelectorAll('.cell');
    cells.forEach(cell => {
        const key = `${cell.dataset.x}${cell.dataset.y}`;
        const piece = boardState[key];

        cell.innerHTML = '';

        if (piece) {
            const pieceEl = document.createElement('div');
            pieceEl.className = `piece ${piece.color}${piece.visible ? '' : ' hidden'}`;
            pieceEl.textContent = piece.visible
                ? (piece.color === 'red' ? PIECE_SYMBOLS[piece.type] : BLACK_SYMBOLS[piece.type])
                : '';
            cell.appendChild(pieceEl);
        }
    });
}

function updateBoardState(from, to, flipResult) {
    const piece = boardState[from];
    if (piece) {
        boardState[to] = {
            color: piece.color,
            type: flipResult || piece.type,
            visible: true
        };
        delete boardState[from];
    }
}

function handleCellClick(cell) {
    if (gameStatus !== 'playing' || currentTurn !== myColor) {
        return;
    }

    if (!selectedCell) {
        const key = `${cell.dataset.x}${cell.dataset.y}`;
        const piece = boardState[key];

        if (piece && piece.color === myColor) {
            selectedCell = cell;
            cell.classList.add('selected');
        }
        return;
    }

    if (selectedCell === cell) {
        selectedCell.classList.remove('selected');
        selectedCell = null;
        return;
    }

    const move = {
        messageType: 'move',
        fromX: selectedCell.dataset.x,
        fromY: Number(selectedCell.dataset.y),
        toX: cell.dataset.x,
        toY: Number(cell.dataset.y),
        isFlip: true
    };

    selectedCell.classList.remove('selected');
    selectedCell = null;
    send(move);
}

function addMoveLog(move, flipResult, capturedPiece) {
    const from = `${move.fromX}${move.fromY}`;
    const to = `${move.toX}${move.toY}`;
    let text = `${from} -> ${to}`;

    if (flipResult) {
        text += ` (翻出: ${PIECE_SYMBOLS[flipResult]})`;
    }
    if (capturedPiece) {
        text += ` (吃: ${PIECE_SYMBOLS[capturedPiece]})`;
    }

    const div = document.createElement('div');
    div.textContent = text;
    moveLogEl.prepend(div);
}

function setButtonStates(states) {
    if (states.startMatch !== undefined) {
        document.getElementById('startMatchButton').disabled = !states.startMatch;
    }
    if (states.cancelMatch !== undefined) {
        document.getElementById('cancelMatchButton').disabled = !states.cancelMatch;
    }
    if (states.ready !== undefined) {
        document.getElementById('readyButton').disabled = !states.ready;
        document.getElementById('readyButton').textContent = states.ready ? '准备' : '已准备';
        myReady = !states.ready;
    }
    if (states.resign !== undefined) {
        document.getElementById('resignButton').disabled = !states.resign;
    }
}

function updateTurnIndicator() {
    document.body.classList.remove('turn-red', 'turn-black');
    document.body.classList.add(`turn-${currentTurn}`);
}

function startTimer() {
    clearInterval(timerInterval);
    deadlineTime = Date.now() + 60000;

    timerInterval = setInterval(() => {
        const remaining = deadlineTime - Date.now();
        if (remaining <= 0) {
            timeLeftEl.textContent = '剩余时间: 00:00';
            clearInterval(timerInterval);
            return;
        }

        const seconds = Math.floor(remaining / 1000);
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        timeLeftEl.textContent = `剩余时间: ${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }, 1000);
}

function startHeartbeat() {
    heartbeatInterval = setInterval(() => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            send({ messageType: 'ping' });
        }
    }, 10000);
}

function clearTimers() {
    clearInterval(timerInterval);
    clearInterval(heartbeatInterval);
}

function showLoginStatus(msg, type) {
    const el = document.getElementById('loginStatus');
    el.textContent = msg;
    el.className = `status ${type}`;
}

function logout() {
    if (socket) {
        socket.close();
    }
    clearTimers();
    loggedInUser = null;
    myColor = null;
    gameStatus = 'waiting';
    boardState = {};

    gameScreen.classList.remove('active');
    loginScreen.classList.add('active');
    document.getElementById('userId').value = '';
    document.getElementById('passWord').value = '';
    document.getElementById('nickName').value = '';
    showLoginStatus('', '');

    redPlayerEl.textContent = '红方: 等待...';
    blackPlayerEl.textContent = '黑方: 等待...';
    statusLine.textContent = '未开始';
    currentTurnEl.textContent = '等待匹配';
    yourColorEl.textContent = '你的颜色: 未知';
    timeLeftEl.textContent = '剩余时间: --:--';
    messageEl.textContent = '点击"开始匹配"寻找对手';
    moveLogEl.innerHTML = '';
    board.innerHTML = '';
}

function renderEmptyBoard() {
    board.replaceChildren();
    for (let y = 9; y >= 0; y--) {
        for (let col = 0; col < 9; col++) {
            const x = String.fromCharCode('a'.charCodeAt(0) + col);
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.dataset.x = x;
            cell.dataset.y = String(y);
            cell.dataset.coord = `${x}${y}`;
            cell.addEventListener('click', () => handleCellClick(cell));
            board.appendChild(cell);
        }
    }
}

renderEmptyBoard();