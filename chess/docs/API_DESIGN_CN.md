# 接口设计说明（中文审阅版）

这份文档是给你审阅用的中文说明。正式实现时仍以 `API_DESIGN.md` 为主。

## 1. 接口文档的作用

这个接口文档规定“前端客户端”和“后端服务器”之间怎么通信。

前端需要看它来知道：

- 什么时候发什么 JSON。
- 收到服务器消息后怎么刷新界面。
- 出错时怎么提示用户。

后端需要看它来知道：

- 收到某种 `messageType` 后该怎么处理。
- 成功、失败、结束时该返回什么 JSON。
- 哪些信息不能泄露给客户端。

## 2. 通信方式

使用：

```text
WebSocket + JSON
```

默认地址：

```text
ws://localhost:8887
```

如果以后要在同一个局域网里连接别人的服务器，可以改成：

```text
ws://对方IP:8887
```

当前版本只支持根地址 `ws://host:8887`，不再额外提供其他 WebSocket 路径别名。

所以前端最好不要把连接地址写死成只能 `localhost`。

## 3. 基本格式

所有消息都是 JSON，并且必须有：

```json
{
  "messageType": "..."
}
```

`messageType` 决定这条消息是什么类型。

例如：

```json
{
  "messageType": "startMatch"
}
```

## 4. 第一版要实现哪些消息

前端发给后端：

```text
startMatch    开始匹配
Ready         准备
move          走子
Resign        认输
```

后端发给前端：

```text
matchSuccess  匹配成功
roomInfo      房间状态
gameStart     游戏开始
moveResult    走子结果
timeout       超时
gameOver      游戏结束
error         协议错误
```

第一版不做：

```text
Login/register 登录注册
cancelMatch 取消匹配
requestFirstHand 请求先手
ping/pong 心跳
旁观
聊天
```

如果收到不支持的消息，服务器不要崩溃，返回 `error code=4002`。

## 5. 坐标规则

协议使用统一的服务器棋盘坐标。

```text
列：a 到 i
行：0 到 9
e0：红帅
e9：黑将
红方在下方
黑方在上方
```

前端可以把“自己一方”显示在下方，但是发给服务器时必须转换回统一坐标。

也就是说：

```text
UI 怎么显示是前端自己的事
WebSocket 里传的坐标必须统一
```

## 6. 棋子类型

JSON 里使用英文字符串：

```text
King
Rook
Knight
Cannon
Pawn
Guard
Bishop
NULL
```

`NULL` 表示这个接收方不应该知道真实棋子类型。

比如：一枚暗子被吃了，吃子方可以知道它是什么；被吃方不应该知道，就给 `NULL`。

Java 内部可以保留数字编码：

```text
0 King
1 Rook
2 Knight
3 Cannon
4 Pawn
5 Guard
6 Bishop
```

但 WebSocket JSON 对外不要发数字，统一发英文字符串。

如果棋盘格子的 `visible=false`，`piece` 表示“这个位置原本对应的走法类型”，不是暗子的真实身份。

例如 `a0` 是红方原本车的位置：

```json
{
  "x": "a",
  "y": 0,
  "piece": "Rook",
  "color": "red",
  "visible": false
}
```

它表示这个暗子第一步按车走，但不表示它真实翻出来一定是车。

如果 `visible=true`，`piece` 才表示真实类型。

## 7. 走子消息

前端走子时发送：

```json
{
  "messageType": "move",
  "fromX": "b",
  "fromY": 3,
  "toX": "b",
  "toY": 4,
  "isFlip": true
}
```

字段含义：

```text
fromX/fromY：起点
toX/toY：终点
isFlip：这步是否导致暗子翻明
```

注意：

```text
isFlip 只是客户端的提示，服务器最终说了算。
不允许原地翻子，所以 from 和 to 不能是同一个格子。
```

服务端返回 `moveResult` 时：

```text
flipResult 表示“移动的那个暗子翻成了什么”。
capturedPiece 表示“本步吃掉的棋子是什么”。
```

## 8. 匹配流程

第一个客户端发：

```json
{
  "messageType": "startMatch"
}
```

服务器让它等待。

第二个客户端也发：

```json
{
  "messageType": "startMatch"
}
```

服务器创建一局游戏，然后给双方发：

```json
{
  "messageType": "matchSuccess",
  "roomId": "room-1",
  "opponentId": "session-2",
  "opponentNickname": "Player 2"
}
```

如果其中一方先发送 `Ready`，服务器会给另一方补发：

```json
{
  "messageType": "roomInfo",
  "opponentReady": true
}
```

这里的意思是：“你的对手已经准备好了”。

然后再发：

```json
{
  "messageType": "gameStart",
  "redPlayerId": "session-1",
  "blackPlayerId": "session-2",
  "yourColor": "red",
  "firstHand": true,
  "initialBoard": [
    {
      "x": "a",
      "y": 0,
      "piece": "Rook",
      "color": "red",
      "visible": false
    },
    {
      "x": "b",
      "y": 0,
      "piece": "Knight",
      "color": "red",
      "visible": false
    },
    {
      "x": "e",
      "y": 0,
      "piece": "King",
      "color": "red",
      "visible": true
    },
    {
      "x": "e",
      "y": 9,
      "piece": "King",
      "color": "black",
      "visible": true
    }
  ]
}
```

其中 `yourColor` 每个客户端收到的不一样。

`initialBoard` 正式实现时应包含双方所有初始有棋子的格子，空格可以不发。暗子的 `piece` 是原位置走法类型，不是真实暗子身份。

## 9. 走子结果

如果走子成功，服务器给双方广播：

```json
{
  "messageType": "moveResult",
  "success": true,
  "valid": true,
  "move": {
    "fromX": "b",
    "fromY": 3,
    "toX": "b",
    "toY": 4,
    "isFlip": true
  },
  "flipResult": "Rook",
  "capturedPiece": null
}
```

如果走子失败，只发给走子的人：

```json
{
  "messageType": "moveResult",
  "success": false,
  "valid": false,
  "code": 2001,
  "message": "illegal move"
}
```

失败时棋盘不变，回合也不变。

## 10. 吃暗子后的信息隐藏

这是接口里必须特别注意的地方。

使用 `capturedPiece` 表示本步被吃掉的棋子类型。

它不是公共接口原本定义的基础字段，而是为了补足公共接口没有表达清楚的“吃子信息”。

如果红方吃掉了黑方一个暗子：

- 红方收到的 `capturedPiece` 可以是真实类型，例如 `"Cannon"`。
- 黑方收到的 `capturedPiece` 应该是 `"NULL"`。

所以同一步棋，服务器可能要给两个客户端发不同的 `moveResult`。

规则：

```text
没有吃子：capturedPiece = null
吃明子：capturedPiece = 被吃明子的真实类型，双方相同
吃暗子，发给吃子方：capturedPiece = 被吃暗子的真实类型
吃暗子，发给被吃方：capturedPiece = "NULL"
```

如果本步既是“己方暗子第一次移动”又“吃掉对方暗子”：

```text
flipResult：表示己方移动的暗子翻成了什么。
capturedPiece：表示被吃掉的对方棋子是什么，或者对不该知道的一方给 NULL。
```

严格只认识公共接口基础字段的客户端可以忽略 `capturedPiece`，仍然能根据 `move` 更新棋盘；只是无法显示“我刚才吃掉的棋子是什么”。
当前版本不再发送 `nextTurn`；客户端应根据 `gameStart`、合法 `moveResult` 和终局消息维护回合显示。

## 11. 游戏结束

例如吃掉将帅：

```json
{
  "messageType": "gameOver",
  "winner": "red",
  "reason": "checkmate",
  "winnerId": "session-1"
}
```

第一版结束原因：

```text
checkmate       将死或吃将帅获胜，对外兼容公共接口
resign          认输
```

超时结束使用单独的 `timeout` 消息，不在第一版额外发送 `gameOver reason=timeout`，这样更贴近公共接口。

## 12. 错误码怎么用

我们不用统一 `Result<T>` 包一层。

也就是说，不这样写：

```json
{
  "code": 0,
  "data": {
    "messageType": "moveResult"
  }
}
```

而是直接按公共接口返回：

```json
{
  "messageType": "moveResult",
  "success": false,
  "valid": false,
  "code": 2001,
  "message": "illegal move"
}
```

错误码沿用公共接口：

```text
2001 非法走子
2002 未轮到本方走子
2003 超时
3001 房间不存在
3002 匹配失败
4001 JSON 格式错误
4002 未知或不支持的 messageType
5000 服务器未知异常
```

普通业务错误不要变成 HTTP 500。

## 13. 互操作怎么体现

互操作的意思是：别组客户端理论上可以连接我们的服务器；我们的客户端理论上也可以连接别组服务器。

我们通过这些方式体现：

- 使用 WebSocket。
- 使用端口 `8887`。
- 使用 JSON。
- 所有消息都有 `messageType`。
- 走子字段使用公共接口的 `fromX/fromY/toX/toY/isFlip`。
- 棋子类型使用公共英文名。
- 错误码尽量沿用公共接口。
- 不把 Java 内部类直接暴露给前端。

第一版不保证完整支持别组的登录、旁观、聊天等扩展，只保证核心对弈消息尽量兼容。

## 14. 你需要重点审的地方

请重点看这些设计是否能接受：

1. 第一版不做登录注册。
2. 走子接口使用 `fromX/fromY/toX/toY/isFlip`。
3. 坐标规定为 `e0` 红帅，`e9` 黑将。
4. 错误不套统一 `Result<T>`，而是直接返回公共接口格式。
5. 默认 WebSocket 地址用 `ws://host:8887`，当前版本只保留根路径。
6. `matchSuccess` 不带颜色，颜色在 `gameStart` 里发。
7. `initialBoard` 要包含初始有棋子的格子，且暗子不泄露真实身份。
8. 吃暗子时，服务器可能给双方发送不同的 `moveResult`。
9. 使用 `capturedPiece` 表示被吃棋子的可见类型。
10. `Ready` 是必须阶段：`matchSuccess` 之后，双方都发送 `Ready`，服务端才发送 `gameStart` 并正式进入对局。
