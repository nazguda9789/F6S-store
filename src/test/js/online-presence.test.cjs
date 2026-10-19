const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm');
function fixture(){
 const events={},sockets=[];let sent=0,retries=0;
 function SockJS(){this.readyState=0;this.send=()=>{sent++};this.close=()=>{this.readyState=3};sockets.push(this);}
 const client={connect:(headers,ok,fail)=>{client.ok=ok;client.fail=fail},subscribe:()=>{}};
 const window={SockJS,Stomp:{over:()=>client},addEventListener:(name,handler)=>events[name]=handler};
 vm.runInNewContext(fs.readFileSync('src/main/webapp/js/online-presence.js','utf8'),
  {window,SockJS,Stomp:window.Stomp,setTimeout:()=>{retries++},clearTimeout:()=>{}});
 return {events,sockets,client,sent:()=>sent,retries:()=>retries};
}
test('presence never sends a STOMP frame before the transport opens',()=>{
 const f=fixture();const socket=f.sockets[0];socket.send('CONNECT');assert.equal(f.sent(),0);
 socket.readyState=1;socket.send('CONNECT');assert.equal(f.sent(),1);
});
test('leaving the page stops sends and reconnect attempts',()=>{
 const f=fixture();const socket=f.sockets[0];socket.readyState=1;f.events.pagehide();
 socket.send('SUBSCRIBE');f.client.fail();assert.equal(f.sent(),0);assert.equal(f.retries(),0);
});
test('restoring a cached page creates a fresh presence connection',()=>{
 const f=fixture();f.events.pagehide();f.events.pageshow({persisted:true});assert.equal(f.sockets.length,2);
});
