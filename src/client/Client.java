package client;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;

public class Client extends JFrame {
    private JTextArea chatArea;
    private JTextArea nicknameArea;
    private JTextField chatInputField;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Color currentColor = Color.BLACK;
    private boolean eraseMode = false;
    private String nickname;
    private JPanel drawingPanel;
    private Point startPoint;
    private JButton readyButton;
    private JTextField wordField;
    public Client() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("캐치마인드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 윈도우창 종료 시 프로세스까지 종료
        setSize(400, 300); // 프레임 사이즈
        setLocationRelativeTo(null); // 창을 모니터 중앙에 배치
        setLayout(null); // 레이아웃을 null로 설정

        // 캐치마인드 대기방 라벨
        JLabel titleLabel = new JLabel("캐치마인드 대기방");
        titleLabel.setBounds(150, 60, 200, 25);
        add(titleLabel);
        nicknameArea = new JTextArea();
        // 게임 시작 버튼
        JButton startButton = new JButton("게임 시작");
        startButton.setBounds(150, 160, 100, 25);
        add(startButton);
        // 버튼 기능
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 게임 시작 버튼 클릭 시 닉네임 입력 화면 표시
                String inputNickname = JOptionPane.showInputDialog("닉네임을 입력하세요:");
                if (inputNickname != null && !inputNickname.isEmpty()) {
                    nickname = inputNickname; // 닉네임 멤버 변수에 저장
                    try {
                        openGameScreen(nickname);
                        connectToServer("127.0.0.1", nickname);
                        setVisible(false);
                    } catch (Exception ex) {

                    }
                }
            }
        });

        setVisible(true);
    }

    private void openGameScreen(String nickname) {
        // 게임화면 창을 띄우는 frame
        JFrame gameFrame = new JFrame("캐치마인드 게임 화면");
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setSize(800, 600);
        gameFrame.getContentPane().setLayout(null);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);
        BasicStroke stroke = new BasicStroke(5.0f); // 선 굵기

        // 그림판
        // drawingPanel 초기화
        drawingPanel = new JPanel() {
            Point startPoint; // 마우스를 누르는 시작점의 좌표를 저장할 Point 객체를 선언
            {
                // 마우스 이벤트를 처리하기 위해 MouseAdapter를 사용
                // 여기서는 마우스를 누를 때(mousePressed)의 이벤트만을 처리,
                // 눌러진 위치의 좌표를 startPoint에 저장
                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        startPoint = e.getPoint();
                    }
                });
                // 마우스의 움직임을 처리하기 위해 MouseMotionAdapter를 사용
                // 여기서는 마우스를 드래그할 때(mouseDragged)의 이벤트를 처리
                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        Graphics g = getGraphics();
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setColor(currentColor);
                        if (eraseMode) {
                            g2d.setColor(getBackground());
                        }
                        g2d.setStroke(stroke);
                        g2d.drawLine(startPoint.x, startPoint.y, e.getX(), e.getY());
                        startPoint = e.getPoint();
                    }
                });
                // 마우스 이벤트 수신을 위한 부분
                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        Graphics g = getGraphics();
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setColor(currentColor);
                        if (eraseMode) {
                            g2d.setColor(getBackground());
                        }
                        g2d.setStroke(stroke);
                        g2d.drawLine(startPoint.x, startPoint.y, e.getX(), e.getY());

                        // 서버로 그림 데이터 전송
                        sendDrawingDataToServer(startPoint.x, startPoint.y, e.getX(), e.getY());
                        startPoint = e.getPoint();
                    }
                });
            }
        };
        drawingPanel.setBorder(new LineBorder(new Color(0, 0, 0))); // 그림판 경계선 설정

        // 그림판 크기, 위치 설정
        drawingPanel.setLayout(null);
        drawingPanel.setBounds(0, 0, 600, 400);
        drawingPanel.setBackground(Color.WHITE);

        // 검은색 펜 버튼
        JButton blackPenButton = new JButton("검은색 펜");
        blackPenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentColor = Color.BLACK;
                eraseMode = false;
                out.println("black");
                out.flush();
            }
        });

        // 지우개 버튼
        JButton eraserButton = new JButton("지우개");
        eraserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentColor = drawingPanel.getBackground();
                eraseMode = true;
                out.println("erase");
                out.flush();
            }
        });

        // 준비 버튼
        readyButton = new JButton("준비");
        readyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println("ready");
                out.flush();
            }
        });

        // 나가기 버튼
        JButton exitButton = new JButton("나가기");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println("exit");
                out.flush();
                System.exit(0); // 나가기 버튼 누르면 종료
            }
        });

        // 버튼들 크기, 위치 설정
        blackPenButton.setLayout(null);
        blackPenButton.setBounds(600, 0, 92, 50);
        eraserButton.setLayout(null);
        eraserButton.setBounds(693, 0, 99, 50);
        readyButton.setLayout(null);
        readyButton.setBounds(600, 300, 99, 50);
        exitButton.setLayout(null);
        exitButton.setBounds(693, 300, 99, 50);

        // 제시어 :
        JLabel wordLabel = new JLabel("제시어 : ");
        wordLabel.setBounds(600, 212, 57, 75);
        // 제시어 출력 창
        wordField = new JTextField(10);
        wordField.setBackground(Color.WHITE);
        wordField.setEditable(false); // 편집 불가능하게 설정
        wordField.setText(""); // setText()메소드로 텍스트 필드 안에 글씨 삽입 가능
        wordField.setFont(new Font("굴림", Font.BOLD, 18));
        wordField.setHorizontalAlignment(SwingConstants.CENTER);
        wordField.setBounds(654, 225, 107, 50);

        // 유저 닉네임 출력 창
        nicknameArea.setEditable(false); // 편집 불가능하게 설정
        nicknameArea.setBounds(12, 0, 162, 143);
        JPanel nicknamePanel = new JPanel(); // 닉네임 판넬
        nicknamePanel.setBackground(Color.WHITE);
        nicknamePanel.add(nicknameArea); // 닉네임 판넬에 nicknameArea 추가
        nicknamePanel.setLayout(null);
        nicknamePanel.setBounds(610, 60, 164, 143);

        // 채팅을 출력하는 창
        chatArea = new JTextArea(); // 채팅을 출력할 JTextArea
        chatArea.setEditable(false); // 편집 불가능하게 설정
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); //스크롤 바가 항상 아래에 위치하게 설정
        chatArea.setBounds(0, 0, 774, 103);
        JScrollPane chatScrollPane = new JScrollPane(chatArea); // 스크롤 추가
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setBounds(0, 0, 774, 103);
        JPanel chatPanel = new JPanel();
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setLayout(null);
        chatPanel.setBounds(0, 402, 774, 103); // setBounds를 사용하여 절대 위치 지정
        chatPanel.add(chatScrollPane);

        // 채팅을 입력하는 텍스트 필드
        JTextField chatInputField = new JTextField(); // 채팅을 입력할 JTextField
        chatInputField.setBounds(0, 0, 733, 40);
        chatInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = chatInputField.getText();
                if (!message.isEmpty()) {
                    out.println("chat:" + nickname +":"+ message); // 서버로 메시지 전송
                    chatInputField.setText(""); // 입력 필드 초기화
                }
            }
        });

        // 텍스트 필드를 판넬에 추가
        JPanel chatInputPanel = new JPanel();
        chatInputPanel.setBackground(Color.WHITE);
        chatInputPanel.setLayout(null);
        chatInputPanel.setBounds(10, 513, 733, 40);
        chatInputPanel.add(chatInputField);

        // gameFrame에 판넬들 추가
        gameFrame.getContentPane().add(drawingPanel);
        gameFrame.getContentPane().add(blackPenButton);
        gameFrame.getContentPane().add(eraserButton);
        gameFrame.getContentPane().add(readyButton);
        gameFrame.getContentPane().add(exitButton);
        gameFrame.getContentPane().add(nicknamePanel);
        gameFrame.getContentPane().add(chatPanel);
        gameFrame.getContentPane().add(chatInputPanel);
        gameFrame.getContentPane().add(wordLabel);
        gameFrame.getContentPane().add(wordField);
    }

    // 채팅 출력
    private void appendChatMessage(String nickname, String message) {
        chatArea.append(nickname + ": " + message + "\n");
    }

    private void connectToServer(String serverAddress, String nickname){
        try {
            // 메시지 수신을 위한 스레드
            socket = new Socket(serverAddress, 9999);
            out = new PrintWriter(socket.getOutputStream(), true);
            InputThread inputThread = new InputThread();
            inputThread.start();
        }catch (Exception ex){

        }
    }

    class InputThread extends Thread {
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("nickname:" + nickname); //처음 한번 서버에 닉네임 보내기
                String line = null;
                String[] parsMsg;
                while ((line = in.readLine()) != null) {
                    System.out.println("수신된 메세지 --> " + line);
                    parsMsg = line.split(":");
                        if (parsMsg[0].equals("0")) {
                            // 닉네임 메시지인 경우
                            nicknameArea.setText(""); // nicknameArea 초기화
                            String nicknameMsg = parsMsg[1]; // "nickname:" 제거
                            String[] nicknames = nicknameMsg.split(","); // 쉼표로 구분된 닉네임을 배열로 변환
                            for (String nickname : nicknames) {
                                if (!nickname.isEmpty()) {
                                    nicknameArea.append(nickname + "\n"); // 각 닉네임을 nicknameArea에 추가
                                }
                            }
                        } else if (parsMsg[0].equals("chatmessage")) {
                            // 일반 채팅 메시지인 경우
                            appendChatMessage(parsMsg[1],parsMsg[2]);
                        }else if (parsMsg[0].equals("server")) {
                            // 서버 알람 메시지인 경우
                            chatArea.append(parsMsg[1] + "\n");
                        }
                        else if (parsMsg[0].equals("word")) {
                            // 제시어가 온 경우
                            if(parsMsg[1].equals(nickname))
                                wordField.setText(parsMsg[2]);
                        }
                        else if (parsMsg[0].equals("GameStart")) {
                            wordField.setText(""); // 초기화
                            String Player = parsMsg[1];
                            readyButton.setVisible(false);
                            // 그림판 내용 지우기
                            Graphics g = drawingPanel.getGraphics();
                            g.setColor(Color.WHITE);
                            g.fillRect(0, 0, drawingPanel.getWidth(), drawingPanel.getHeight());
                            drawingPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
                            if (Player.equals(nickname)) {
                                // 이 클라이언트가 첫 번째 플레이어인 경우
                                // 그림판 수정 가능
                                enableDrawingPanel();
                            } else {
                                // 그림판 수정 불가능
                                disableDrawingPanel();
                            }
                        } else if (parsMsg[0].equals("draw")) {
                            processDrawingData(parsMsg[1]);
                        }
                        else if (parsMsg[0].equals("black")) {
                            // 검은펜
                            currentColor = Color.BLACK;
                        }
                        else if (parsMsg[0].equals("erase")) {
                            // 지우개
                            currentColor = Color.WHITE;
                        }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // 그림 데이터 처리 메소드
    private void processDrawingData(String drawingData) {
        // drawingData 예시: "draw:x1,y1,x2,y2"
        String[] parts = drawingData.split(",");
        int x1 = Integer.parseInt(parts[0]);
        int y1 = Integer.parseInt(parts[1]);
        int x2 = Integer.parseInt(parts[2]);
        int y2 = Integer.parseInt(parts[3]);

        Graphics g = drawingPanel.getGraphics();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(5.0f)); // 여기서 선의 두께를 조절할 수 있습니다
        g2d.drawLine(x1, y1, x2, y2);
        g.dispose();
    }
    private void sendDrawingDataToServer(int x1, int y1, int x2, int y2) {
        // RGB 값과 선 굵기를 포함한 데이터 포맷
        String drawingData = String.format("draw:%d,%d,%d,%d", x1, y1, x2, y2);
        out.println(drawingData);
        out.flush();
    }

    private void enableDrawingPanel() {
        drawingPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
            }
        });
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Graphics g = drawingPanel.getGraphics();
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(currentColor);
                g2d.setStroke(new BasicStroke(5.0f));
                g2d.drawLine(startPoint.x, startPoint.y, e.getX(), e.getY());
                startPoint = e.getPoint();
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Graphics g = getGraphics();
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(currentColor);
                if (eraseMode) {
                    g2d.setColor(getBackground());
                }
                g2d.setStroke(new BasicStroke(5.0f));
                g2d.drawLine(startPoint.x, startPoint.y, e.getX(), e.getY());

                // 서버로 그림 데이터 전송
                sendDrawingDataToServer(startPoint.x, startPoint.y, e.getX(), e.getY());

                startPoint = e.getPoint();
            }
        });
    }

    private void disableDrawingPanel() {
        for (MouseListener listener : drawingPanel.getMouseListeners()) {
            drawingPanel.removeMouseListener(listener);
        }
        for (MouseMotionListener listener : drawingPanel.getMouseMotionListeners()) {
            drawingPanel.removeMouseMotionListener(listener);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client();
        });
    }
}