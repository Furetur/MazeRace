// @ts-ignore
import {WebSocket} from "https://deno.land/x/websocket@v0.0.5/mod.ts";
import {ask} from "./ask.ts";

const directions = {
    'w': 'UP',
    'a': 'LEFT',
    's': 'DOWN',
    'd': 'RIGHT',
}



async function cli() {
    const roomId = await ask("Room id")
    const endpoint = roomId === "" ? "http://localhost:8080/ws/room" : `http://localhost:8080/ws/room?id=${roomId}`
    const ws = new WebSocket(endpoint)

    ws.on('error', (e: Error) => console.log('error', e))
    ws.on('close', function(event: any) {
        console.log('disconnected', event)
    })
    ws.on('open', () => console.log('connected'))

    ws.on('message', (msg: any) => {
        console.log('Received', JSON.parse(msg))
    })

    async function send(obj: Object) {
        console.log('Sent', obj)
        ws.send(JSON.stringify(obj))
    }

    while (true) {
        const input = await ask()
        if (input === 'ready') {
            send({
                type: 'com.example.ReadyUpdate',
                isReady: true,
            })
        } else if (input === 'unready') {
            send({
                type: 'com.example.ReadyUpdate',
                isReady: false,
            })
        } else if (input in directions) {
            send({
                type: 'com.example.PlayerInput',
                // @ts-ignore
                direction: directions[input]
            })
        }
    }
}

cli()