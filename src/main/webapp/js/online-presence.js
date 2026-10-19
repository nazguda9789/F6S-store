// Presence is optional; rapid navigation must not send STOMP frames on a closed socket.
(() => {
    if (!window.SockJS || !window.Stomp || window.f6sPresenceStarted) return;
    window.f6sPresenceStarted = true;
    let stopped = false, socket, timer;
    const connect = () => {
        if (stopped) return;
        socket = new SockJS('/ws');
        const transport = socket;
        const send = transport.send.bind(transport);
        transport.send = data => {
            if (!stopped && transport.readyState === 1) send(data);
        };
        const client = Stomp.over(transport);
        client.debug = () => {};
        client.connect({}, () => {
            if (!stopped && transport.readyState === 1) client.subscribe('/topic/online', () => {});
        }, () => {
            if (!stopped) timer = setTimeout(connect, 5000);
        });
    };
    window.addEventListener('pagehide', () => {
        stopped = true;
        clearTimeout(timer);
        if (socket) {
            socket.onopen = null;
            socket.onclose = null;
            socket.close();
        }
    });
    window.addEventListener('pageshow', event => {
        if (event.persisted) { stopped = false; connect(); }
    });
    connect();
})();
