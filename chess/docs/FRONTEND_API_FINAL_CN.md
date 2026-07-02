# 前后端联调接口文档（最终版，按当前代码实现）

这份文档只描述当前后端代码真实实现出来的接口、字段和调用顺序。

目的不是追求和公共接口完全一致，而是保证前端可以按照这份文档直接联调。

如果代码与旧文档、课件、公共接口有冲突，以这份文档和当前服务端代码为准。

## 1. 总体说明

### 1.1 通信方式

- 协议：WebSocket
- 服务端端口：`8887`
- WebSocket 路径：`/`

默认地址：

```text
ws://localhost:8887/
```

也可以写成：

```text
ws://localhost:8887
```

当前后端只监听根路径，不使用 `/ws/game` 等额外路径。

### 1.2 消息格式

所有消息都是 JSON，并且必须包含 `messageType` 字段。

示例：

```json
{
  "messageType": "Login"
}
```

### 1.3 当前已实现消息

客户端发服务端：

- `register`
- `Login`
- `startMatch`
- `Ready`
- `move`
- `Resign`

服务端发客户端：

- `loginResult`
- `matchSuccess`
- `roomInfo`
- `gameStart`
- `moveResult`
- `timeout`
- `gameOver`
- `error`

当前不处理的消息：

- `ping`
- `pong`
- `cancelMatch`
- `requestFirstHand`

## 2. 客户端状态流转

### 2.1 推荐状态

- `DISCONNECTED`：未连接
- `CONNECTED_IDLE`：已连接，但未登录
- `LOGGED_IN_IDLE`：已登录，但未匹配
- `MATCHING`：已发送 `startMatch`，等待第二位玩家
- `PREPARING`：匹配成功，等待双方准备
- `PLAYING`：对局中
- `ENDED`：对局结束

### 2.2 标准时序

```text
1. 建立 WebSocket 连接
2. 发送 register 或 Login
3. 收到 loginResult
4. 登录成功后发送 startMatch
5. 若当前只有一个人，服务端暂时不回消息，客户端保持 MATCHING
6. 第二位玩家进入后，服务端给双方发送 matchSuccess
7. 双方分别发送 Ready
8. 若只有一方先准备，服务端给另一方发送 roomInfo
9. 当双方都 Ready 后，服务端给双方发送 gameStart
10. 进入 PLAYING，当前轮到谁走，谁就发送 move
11. 服务端返回 moveResult
12. 若吃将结束，服务端继续发送 gameOver
13. 若有人认输，服务端发送 gameOver
14. 若有人超时，服务端发送 timeout
15. 客户端进入 ENDED
```

## 3. 客户端发服务端

## 3.1 register

用途：注册新账号。注册成功后，服务端会直接把当前连接视为已登录。

```json
{
  "messageType": "register",
  "userId": "alice",
  "passWord": "123456",
  "nickName": "Alice"
}
```

字段说明：

- `userId`：登录账号，必须唯一
- `passWord`：密码
- `nickName`：显示昵称

前端调用时机：

- WebSocket 已连接
- 用户选择注册

服务端行为：

- 若 `userId` 不重复：写入本地用户文件，返回 `loginResult success=true`，并直接视为已登录
- 若 `userId` 已存在：返回 `loginResult success=false`

## 3.2 Login

用途：登录已有账号。

```json
{
  "messageType": "Login",
  "userId": "alice",
  "passWord": "123456"
}
```

前端调用时机：

- WebSocket 已连接
- 用户选择登录

服务端行为：

- 校验 `userId + passWord`
- 成功则返回 `loginResult success=true`，并把当前连接绑定到该用户
- 失败则返回 `loginResult success=false`

## 3.3 startMatch

用途：开始匹配。

```json
{
  "messageType": "startMatch"
}
```

前端调用时机：

- 已登录成功
- 用户点击“开始匹配”

服务端行为：

- 如果当前没有等待者：记为等待中，不立刻回消息
- 如果当前已有一个等待者，且不是自己：创建房间，给双方发送 `matchSuccess`
- 当前版本只支持同时一盘对局

## 3.4 Ready

用途：匹配成功后，告知服务端“我准备好了”。

```json
{
  "messageType": "Ready"
}
```

前端调用时机：

- 已收到 `matchSuccess`
- 处于准备阶段
- 用户点击准备

服务端行为：

- 先准备的一方，不会收到自己的回包
- 服务端会给另一方发送 `roomInfo`
- 当双方都准备后，给双方发送 `gameStart`

## 3.5 move

用途：走子。

```json
{
  "messageType": "move",
  "fromX": "a",
  "fromY": 3,
  "toX": "a",
  "toY": 4,
  "isFlip": false
}
```

字段说明：

- `fromX`：起点列，`a` 到 `i`
- `fromY`：起点行，`0` 到 `9`
- `toX`：终点列，`a` 到 `i`
- `toY`：终点行，`0` 到 `9`
- `isFlip`：客户端提示值，服务端不完全信任

前端调用时机：

- 已收到 `gameStart`
- 当前处于 `PLAYING`
- 当前轮到自己

服务端行为：

- 校验是否已登录
- 校验是否在房间中
- 校验是否轮到当前玩家
- 校验走法是否合法
- 合法则执行走子，回发 `moveResult`
- 若本步结束游戏，还会继续发送 `gameOver`
- 成功走子后重置下一手超时

## 3.6 Resign

用途：认输。

```json
{
  "messageType": "Resign"
}
```

前端调用时机：

- 已开局
- 用户点击认输

服务端行为：

- 当前发送者直接判负
- 给双方发送 `gameOver`

## 4. 服务端发客户端

## 4.1 loginResult

用途：注册结果或登录结果。

### 成功示例

```json
{
  "messageType": "loginResult",
  "success": true,
  "reason": "ok",
  "userId": "alice",
  "nickName": "Alice"
}
```

### 失败示例

```json
{
  "messageType": "loginResult",
  "success": false,
  "reason": "user already exists",
  "userId": null,
  "nickName": null
}
```

当前 `reason` 可能值：

- `ok`
- `user already exists`
- `wrong userId or passWord`

前端收到后应该做什么：

- `success=true`：进入 `LOGGED_IN_IDLE`，保存自己的 `userId` 和 `nickName`
- `success=false`：保持未登录状态，并提示失败原因

## 4.2 matchSuccess

用途：匹配成功。

示例：

```json
{
  "messageType": "matchSuccess",
  "roomId": "room-1",
  "opponentId": "bob",
  "opponentNickname": "Bob"
}
```

字段说明：

- `roomId`：当前固定为 `room-1`
- `opponentId`：对手真实 `userId`
- `opponentNickname`：对手真实 `nickName`

前端收到后应该做什么：

- 从 `MATCHING` 切到 `PREPARING`
- 显示对手信息
- 显示准备按钮

## 4.3 roomInfo

用途：准备阶段房间状态通知。

示例：

```json
{
  "messageType": "roomInfo",
  "opponentReady": true
}
```

当前实现语义：

- 只在准备阶段使用
- 只会在“对手已点击 Ready”时发给你
- 当前版本里这条消息出现时，`opponentReady` 实际上就是 `true`

## 4.4 gameStart

用途：双方都准备后，正式开局。

示例：

```json
{
  "messageType": "gameStart",
  "redPlayerId": "alice",
  "blackPlayerId": "bob",
  "yourColor": "red",
  "firstHand": true,
  "initialBoard": [
    {
      "x": "a",
      "y": 0,
      "color": "RED",
      "piece": "ROOK",
      "visible": false
    }
  ]
}
```

字段说明：

- `redPlayerId`：红方真实 `userId`
- `blackPlayerId`：黑方真实 `userId`
- `yourColor`：`red` 或 `black`
- `firstHand`：是否先手；红方为 `true`，黑方为 `false`
- `initialBoard`：初始棋盘上所有非空格子

`initialBoard` 每项字段：

- `x`：列
- `y`：行
- `color`：当前代码实际返回 `RED` / `BLACK`
- `piece`：当前代码实际返回 `KING` / `ROOK` / `KNIGHT` / `CANNON` / `PAWN` / `GUARD` / `BISHOP`
- `visible`：是否明牌

## 4.5 moveResult

用途：走子结果。

### 成功示例

```json
{
  "messageType": "moveResult",
  "success": true,
  "move": {
    "fromX": "a",
    "fromY": 3,
    "toX": "a",
    "toY": 4,
    "flip": false
  },
  "valid": true
}
```

### 失败示例

```json
{
  "messageType": "moveResult",
  "success": false,
  "move": {
    "fromX": "a",
    "fromY": 3,
    "toX": "a",
    "toY": 4,
    "flip": false
  },
  "flipResult": null,
  "valid": false,
  "code": 2001,
  "message": "illegal movement",
  "capturedPiece": null
}
```

字段说明：

- `success`：是否成功
- `move`：服务端确认后的走子
- `flipResult`：若本步翻出移动暗子，则返回真实类型；如果没有翻子，这个字段会直接省略
- `valid`：成功时为 `true`，失败时为 `false`
- `code`：只在失败时出现
- `message`：只在失败时出现
- `capturedPiece`：若需要展示被吃棋子类型，则出现；否则这个字段会直接省略

重要注意：

- `move` 中布尔字段序列化后实际叫 `flip`，不是 `isFlip`
- 前端必须按 `flip` 解析
- 当前 `moveResult` 使用“空值不输出”，所以成功消息里很多可选字段可能完全不存在，而不是写成 `null`

隐藏吃子规则：

- 吃明子：双方看到的 `capturedPiece` 一样
- 吃暗子：行动方能看到真实 `capturedPiece`
- 被吃方收到的同一步 `moveResult` 中，`capturedPiece` 字段会被直接省略

## 4.6 gameOver

用途：正常终局消息。

### 认输示例

```json
{
  "messageType": "gameOver",
  "winner": "black",
  "reason": "resign",
  "winnerId": "bob"
}
```

### 吃将示例

```json
{
  "messageType": "gameOver",
  "winner": "red",
  "reason": "checkmate",
  "winnerId": "alice"
}
```

### 无吃子和棋示例

```json
{
  "messageType": "gameOver",
  "winner": null,
  "reason": "drawNoCapture",
  "winnerId": null
}
```

当前 `reason` 可能值：

- `checkmate`
- `resign`
- `drawNoCapture`

## 4.7 timeout

用途：超时结束消息。

示例：

```json
{
  "messageType": "timeout",
  "loserId": "alice",
  "winnerId": "bob",
  "reason": "timeout"
}
```

注意：

- 当前代码超时时只发 `timeout`
- 不再补发 `gameOver`

## 4.8 error

用途：协议级错误或时序错误。

示例：

```json
{
  "messageType": "error",
  "code": 3002,
  "message": "login required"
}
```

当前常见错误码：

- `3002`：未登录就发送必须登录后才能发送的消息
- `3001`：当前玩家不在房间里，却发送了依赖房间的消息
- `4001`：JSON 格式错误
- `4002`：未知 `messageType`
- `5000`：运行时阶段错误，例如开局后重复 `Ready`

## 5. 棋盘与坐标规则

### 5.1 坐标系统

- 列：`a` 到 `i`
- 行：`0` 到 `9`
- `e0` 是红方将帅位置
- `e9` 是黑方将帅位置

服务端统一使用这套全局坐标。

### 5.2 颜色字符串

当前代码里有两种大小写风格：

- `gameStart.yourColor`、`gameOver.winner`：小写 `red` / `black`
- `initialBoard[].color`：大写 `RED` / `BLACK`

### 5.3 棋子类型字符串

当前代码实际返回大写枚举名：

- `KING`
- `ROOK`
- `KNIGHT`
- `CANNON`
- `PAWN`
- `GUARD`
- `BISHOP`

会出现在：

- `initialBoard[].piece`
- `moveResult.flipResult`
- `moveResult.capturedPiece`

## 6. 前端联调建议

### 6.1 最小接入顺序

建议前端按这个顺序做：

1. 建立 WebSocket 连接
2. 发送 `register` 或 `Login`
3. 收到 `loginResult`
4. 发送 `startMatch`
5. 收到 `matchSuccess`
6. 点击准备并发送 `Ready`
7. 收到 `roomInfo`
8. 收到 `gameStart`
9. 发送 `move`
10. 处理 `moveResult`
11. 处理 `gameOver` / `timeout`
12. 处理 `error`

### 6.2 前端不要做的事

- 不要在未登录时发送 `startMatch` / `Ready` / `move` / `Resign`
- 不要自己决定翻子真实结果
- 不要自己决定暗子被吃后的显示结果
- 不要自己判定超时胜负
- 不要跳过 `Ready` 阶段

### 6.3 推荐本地状态字段

- `socketConnected`
- `phase`
  - `DISCONNECTED`
  - `CONNECTED_IDLE`
  - `LOGGED_IN_IDLE`
  - `MATCHING`
  - `PREPARING`
  - `PLAYING`
  - `ENDED`
- `selfUserId`
- `selfNickName`
- `roomId`
- `opponentId`
- `opponentNickname`
- `yourColor`
- `firstHand`
- `opponentReady`
- `board`
- `isYourTurn`
- `lastError`

## 7. 完整时序示例

### 7.1 登录并开局

```text
客户端A连接
客户端B连接

A -> register 或 Login
服务端 -> A: loginResult

B -> register 或 Login
服务端 -> B: loginResult

A -> startMatch
服务端暂不回包

B -> startMatch
服务端 -> A: matchSuccess
服务端 -> B: matchSuccess

A -> Ready
服务端 -> B: roomInfo(opponentReady=true)

B -> Ready
服务端 -> A: gameStart
服务端 -> B: gameStart
```

### 7.2 合法走子

```text
当前走子方 -> move
服务端 -> 双方: moveResult(success=true)
如果未结束，继续下一回合
```

### 7.3 非法走子

```text
当前玩家 -> move
服务端 -> 发送者本人: moveResult(success=false, valid=false, code=...)
```

### 7.4 认输

```text
玩家 -> Resign
服务端 -> 双方: gameOver(reason=resign)
```

### 7.5 超时

```text
服务端内部计时
到时后 -> 双方: timeout
```

## 8. 当前已知实现特征

这些不是理想化设计，而是当前代码的真实行为，前端必须按它适配：

1. `register` 成功后，当前连接会直接视为已登录。
2. 未登录时发送 `startMatch`、`Ready`、`move`、`Resign`，会收到 `error code=3002`。
3. `startMatch` 在只有一个人排队时，不回任何消息。
4. `Ready` 先到的一方，不一定收到自己的回包；通常是另一方收到 `roomInfo`。
5. `moveResult.move` 中的布尔字段名实际是 `flip`，不是 `isFlip`。
6. `initialBoard.color` 是大写 `RED/BLACK`。
7. `piece`、`flipResult`、`capturedPiece` 都是大写枚举名。
8. 吃暗子时，双方收到的 `moveResult.capturedPiece` 不同。
9. 超时只发 `timeout`，不再补 `gameOver`。
10. 当前只支持一个活动房间，房间号固定 `room-1`。

## 9. 一句话联调结论

前端只要严格遵守这个顺序：

```text
连接 -> register/Login -> loginResult -> startMatch -> matchSuccess -> Ready -> roomInfo -> gameStart -> move -> moveResult -> gameOver/timeout
```

就能和当前后端代码正常联调。
