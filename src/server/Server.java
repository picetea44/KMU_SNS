package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ChatThread> clientInfos;
    private ServerSocket serverSocket;
    private Socket socket;
    private int currentPlayerIndex = 0;
    private static int currentWordIndex = 0;
    public Server(){
        try {
            //리스닝 소켓
            serverSocket = new ServerSocket(9999);
            //공유객체에서 쓰레드에 안전한 리스트를 만든다.
            // 이 공유객체는 모든 클라이언트에 일괄적으로 데이터를 보낼때 쓴다.
            clientInfos = new ArrayList<>();
            while (true) {
                //사용자 접속 시 소켓 생성, socket -> 클라이언트와 통신용 소켓
                socket = serverSocket.accept();
                System.out.println("접속 완료 : " + socket);
                // 쓰레드 생성해서 소켓 넘기기
                ChatThread chatThread = new ChatThread(socket);
                // 쓰레드 실행
                chatThread.start();
                clientInfos.add(chatThread);
            }
        }catch (Exception ex){

        }
    }

    //채팅을 위한 내부 스레드
    //스레드 상속 시 run 메소드 필요함
    class ChatThread extends Thread {
        private Socket socket;
        public String nickname;
        private PrintWriter out;
        private BufferedReader in;
        private static final String[] WORD_LIST = {"사과", "바나나", "딸기", "제주도", "통닭", "정회창", "박상엄"};
        private boolean isReady = false;
        public ChatThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String line = null;
                String[] parsMsg;

                while ((line = in.readLine()) != null) {
                    System.out.println("수신된 메세지 --> " + line);
                    if (line.startsWith("nickname:")) {
                        parsMsg = line.split(":");
                        nickname = parsMsg[1];
                        sendAllNicknames(nickname);
                    }
                    else if (line.equals("ready")) {
                        isReady = true;
                        broadcastMessage("server:" + this.nickname + " 님 준비 완료!",null);
                        if (allClientsReady()) {
                            String firstPlayer = clientInfos.get(currentPlayerIndex).nickname; // 첫 번째 클라이언트의 닉네임 받기
                            broadcastMessage("GameStart:"+ firstPlayer, null);
                            broadcastMessage("word:"+firstPlayer+":"+WORD_LIST[currentWordIndex],null);
                            broadcastMessage("server:게임을 시작합니다!", null);
                            broadcastMessage("server:" + firstPlayer + " 님의 차례입니다.", null);
                        }
                    }
                    else if (line.equals("exit")) {
                        // 사용자가 나갔음을 처리
                        clientInfos.removeIf(client -> client.socket.equals(this.socket));
                        broadcastMessage("server:" + nickname + " 님이 접속을 종료하였습니다.", null);
                        sendAllNicknames(null);
                        // 소켓 닫기
                        try {
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break; // while 루프 탈출
                    } else if (line.startsWith("chat:")) {
                        parsMsg = line.split(":"); //메세지 분리

                        String senderNickname = parsMsg[1];
                        String messageContent = parsMsg[2];

                        // 채팅 메시지 처리
                        broadcastMessage("chatmessage:" + senderNickname + ": " + messageContent, null);

                        // 게임 시작 후 채팅에 정답이 있는 경우
                            if (messageContent.equals(WORD_LIST[currentWordIndex])) {
                                // 정답 처리 로직
                                broadcastMessage("server:" + senderNickname + " 님이 정답을 맞췄습니다!",null);
                                moveToNextPlayer();
                                String nextPlayer = clientInfos.get(currentPlayerIndex).nickname;
                                broadcastMessage("GameStart:" + nextPlayer, null);
                                broadcastMessage("server:" + nextPlayer + " 님의 차례 입니다.", null);
                                currentWordIndex = (currentWordIndex + 1) % WORD_LIST.length;
                                broadcastMessage("word:"+nextPlayer+":"+WORD_LIST[currentWordIndex],null);
                            }

                    } else if (line.startsWith("draw:")) {
                        broadcastDrawing(line);
                    }
                    else if (line.startsWith("black")) {
                        broadcastMessage("black:",null);
                    }
                    else if (line.startsWith("erase")) {
                        broadcastMessage("erase:",null);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                // 최종적으로 안전하게 한 번 더 소켓 닫기
                try {
                    if (socket != null) socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        // 그림 데이터 브로드캐스트
        private void broadcastDrawing(String drawingData) {
            for (ChatThread client : clientInfos) {
                if (client != this) { // 데이터를 보낸 클라이언트를 제외하고 전송
                    client.out.println(drawingData);
                    client.out.flush();
                }
            }
        }
        // 다음 플레이어로 넘기는 메서드
        private void moveToNextPlayer() {
            currentPlayerIndex = (currentPlayerIndex + 1) % clientInfos.size();
        }

        private boolean allClientsReady() {
            for (ChatThread client : clientInfos) {
                if (!client.isReady) {
                    return false;
                }
            }
            return true;
        }
        private void sendAllNicknames(String newNickname) {
            synchronized (clientInfos) {
                StringBuilder nicknames = new StringBuilder("0:");
                for (ChatThread client : clientInfos) {
                    nicknames.append(client.nickname).append(",");
                }
                broadcastMessage(nicknames.toString(), null);
                // 새로운 클라이언트의 입장 메시지 브로드캐스트
                if (newNickname != null && !newNickname.isEmpty()) {
                    String ipAddress = socket.getInetAddress().getHostAddress();
                    int port = socket.getPort();
                    String entranceMessage = newNickname + " 님이 접속 하였습니다. IP = " + ipAddress + ", Port = " + port;
                    broadcastMessage("server:" + entranceMessage, null);
                }
            }
        }
        private void broadcastMessage(String message, PrintWriter senderOut) {
            for (ChatThread client : clientInfos) {
                if (client.out != senderOut) {
                    client.out.println(message);
                    client.out.flush();
                }
            }
        }
    }
    public static void main(String[] args){
        new Server();
    }
}