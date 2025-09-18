package view;

import ai.IANivel2;
import model.board.Move;

import controller.Game;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import model.board.Position;
import model.pieces.Pawn;
import model.pieces.Piece;

public class ChessGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final Color LIGHT_SQ = new Color(194, 160, 212);
    private static final Color DARK_SQ  = new Color(50, 6, 95);

    private static final Color HILITE_SELECTED = new Color(200, 120, 250);
    private static final Color HILITE_LEGAL    = new Color(160, 100, 200);
    private static final Color HILITE_LASTMOVE = new Color(255, 200, 100);

    private static final Border BORDER_SELECTED = new MatteBorder(3,3,3,3, HILITE_SELECTED);
    private static final Border BORDER_LEGAL    = new MatteBorder(3,3,3,3, HILITE_LEGAL);
    private static final Border BORDER_LASTMOVE = new MatteBorder(3,3,3,3, HILITE_LASTMOVE);

    private final Game game;

    private final JPanel boardPanel;
    private final JButton[][] squares = new JButton[8][8];

    private final JLabel status;
    private final JTextArea history;
    private final JScrollPane historyScroll;
    private final List<String> capturedWhite = new ArrayList<>();
    private final List<String> capturedBlack = new ArrayList<>();

    private JCheckBoxMenuItem pcAsBlack;
    private JSpinner depthSpinner;
    private JMenuItem newGameItem, quitItem;

    private Position selected = null;
    private List<Position> legalForSelected = new ArrayList<>();

    private Position lastFrom = null, lastTo = null;

    private boolean aiThinking = false;
    private final Random rnd = new Random();

    public ChessGUI() {
        super("My word purple | ChessGame");

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {
        }

        this.game = new Game();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        setJMenuBar(buildMenuBar());

        boardPanel = new JPanel(new GridLayout(8, 8, 0, 0));
        boardPanel.setBackground(DARK_SQ);
        boardPanel.setPreferredSize(new Dimension(920, 680));

        Border outerMargin = BorderFactory.createEmptyBorder(18, 18, 18, 18);
        Border innerBorder = BorderFactory.createMatteBorder(6, 6, 6, 6, LIGHT_SQ);
        boardPanel.setBorder(BorderFactory.createCompoundBorder(outerMargin, innerBorder));

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                final int rr = r;
                final int cc = c;
                JButton b = new JButton();
                b.setMargin(new Insets(0, 0, 0, 0));
                b.setFocusPainted(false);
                b.setOpaque(true);
                b.setBorderPainted(true);
                b.setContentAreaFilled(true);
                b.setFont(b.getFont().deriveFont(Font.BOLD, 24f));
                b.addActionListener(e -> handleClick(new Position(rr, cc)));
                squares[r][c] = b;
                boardPanel.add(b);
            }
        }

        status = new JLabel("Vez: Eu");
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        status.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        status.setForeground(new Color(255, 255, 255));

        history = new JTextArea(14, 22);
        history.setEditable(false);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        history.setBackground(new Color(255, 255, 255));
        historyScroll = new JScrollPane(history);
        historyScroll.setPreferredSize(new Dimension(250, 400));

        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel histLabel = new JLabel("Histórico de lances:");
        histLabel.setFont(new Font("Arial", Font.BOLD, 14));
        histLabel.setForeground(new Color(255, 255, 255));
        histLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        rightPanel.add(histLabel, BorderLayout.NORTH);
        rightPanel.add(historyScroll, BorderLayout.CENTER);
        rightPanel.add(buildSideControls(), BorderLayout.SOUTH);
        rightPanel.setBackground(new Color(91, 126, 167));

        add(boardPanel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refresh();
            }
        });

        setMinimumSize(new Dimension(920, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(47, 46, 71));

        setupAccelerators();

        setVisible(true);
        refresh();
        maybeTriggerAI();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu gameMenu = new JMenu("Menu");
        newGameItem = new JMenuItem("Novo Jogo");
        newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newGameItem.addActionListener(e -> doNewGame());
        pcAsBlack = new JCheckBoxMenuItem("PC joga com a IA");
        pcAsBlack.setSelected(false);
        JMenu depthMenu = new JMenu("Profundidade IA");
        depthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        depthSpinner.setToolTipText("Profundidade efetiva da IA (heurística não-minimax)");
        depthMenu.add(depthSpinner);
        quitItem = new JMenuItem("Sair");
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        quitItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(pcAsBlack);
        gameMenu.add(depthMenu);
        gameMenu.addSeparator();
        gameMenu.add(quitItem);
        mb.add(gameMenu);
        return mb;
    }

    private JPanel buildSideControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setBackground(new Color(91, 126, 167));
        JButton btnNew = new JButton("Novo Jogo");
        btnNew.addActionListener(e -> doNewGame());
        panel.add(btnNew);
        btnNew.setBackground(new Color(120, 40, 40));
        btnNew.setForeground(Color.WHITE);
        btnNew.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JCheckBox cb = new JCheckBox("PC (IA)");
        cb.setSelected(pcAsBlack.isSelected());
        cb.addActionListener(e -> {
            pcAsBlack.setSelected(cb.isSelected());
            maybeTriggerAI();
        });
        panel.add(cb);

        panel.add(new JLabel("Nível IA:"));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        sp.setValue(depthSpinner.getValue());
        sp.addChangeListener(e -> {
            depthSpinner.setValue(sp.getValue());
            maybeTriggerAI();
        });
        panel.add(sp);
        return panel;
    }

    private void setupAccelerators() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                        "newGame");
        getRootPane().getActionMap().put("newGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doNewGame();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                        "quit");
        getRootPane().getActionMap().put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(ChessGUI.this, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private void doNewGame() {
        selected = null;
        legalForSelected.clear();
        lastFrom = lastTo = null;
        aiThinking = false;
        // Limpar as listas de peças capturadas
        capturedWhite.clear();
        capturedBlack.clear();
        game.newGame();
        refresh();
        maybeTriggerAI();
    }

    private void handleClick(Position clicked) {
        if (game.isGameOver() || aiThinking)
            return;

        if (pcAsBlack.isSelected() && !game.whiteToMove())
            return;

        Piece p = game.board().get(clicked);

        if (selected == null) {
            if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            }
        } else {
            if (game.isLegal(selected, clicked)) { // Validação centralizada na classe Game
                Character promo = null;
                Piece moving = game.board().get(selected);
                Piece captured = game.board().get(clicked);
                if (captured != null) {
                    if (captured.isWhite()) {
                        capturedWhite.add(captured.getSymbol());
                    } else {
                        capturedBlack.add(captured.getSymbol());
                    }
                }
                if (moving instanceof Pawn && game.isPromotion(selected, clicked)) {
                    promo = askPromotion();
                }
                lastFrom = selected;
                lastTo = clicked;
                game.move(selected, clicked, promo);
                selected = null;
                legalForSelected.clear();
                refresh();
                maybeAnnounceEnd();
                maybeTriggerAI();
                return;
            } else if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            } else {
                selected = null;
                legalForSelected.clear();
            }
        }
        refresh();
    }

    private Character askPromotion() {
        String[] opts = { "Rainha", "Torre", "Bispo", "Cavalo" };
        int ch = JOptionPane.showOptionDialog(
                this,
                "Escolha a peça para promoção:",
                "Promoção",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opts,
                opts[0]);
        return switch (ch) {
            case 1 -> 'R';
            case 2 -> 'B';
            case 3 -> 'N';
            default -> 'Q';
        };
    }

    private void maybeTriggerAI() {
        if (game.isGameOver())
            return;
        if (!pcAsBlack.isSelected())
            return;
        if (game.whiteToMove())
            return;

        aiThinking = true;
        status.setText("Vez: IA — pensando...");

        new SwingWorker<model.board.Move, Void>() {
            @Override
            protected model.board.Move doInBackground() {
                IANivel2 ia = new IANivel2();
                return ia.makeMove(game);
            }

            @Override
            protected void done() {
                try {
                    model.board.Move bestMove = get();
                    if (bestMove != null && !game.isGameOver() && !game.whiteToMove()) {
                        lastFrom = bestMove.getFrom();
                        lastTo = bestMove.getTo();
                        Character promo = bestMove.getPromotion();
                        game.move(bestMove.getFrom(), bestMove.getTo(), promo);
                    }
                } catch (Exception ignored) {
                } finally {
                    aiThinking = false;
                    refresh();
                    maybeAnnounceEnd();
                }
            }
        }.execute();
    }

    private int evaluateBoard() {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece p = game.board().get(pos);
                if (p != null) {
                    int value = pieceValue(p);
                    if (p.isWhite()) {
                        score += value + centerBonus(pos);
                        score += (7 - r) * 5;
                    } else {
                        score -= value + centerBonus(pos);
                        score -= r * 5;
                    }
                }
            }
        }
        return score;
    }

    private static class Move {
        final Position from, to;

        Move(Position f, Position t) {
            this.from = f;
            this.to = t;
        }
    }

    private List<Move> collectAllLegalMovesForSide(boolean whiteSide) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                Piece piece = game.board().get(from);
                if (piece != null && piece.isWhite() == whiteSide) {
                    for (Position to : game.legalMovesFrom(from)) {
                        moves.add(new Move(from, to));
                    }
                }
            }
        }
        return moves;
    }

    private int pieceValue(Piece p) {
        if (p == null)
            return 0;
        switch (p.getSymbol()) {
            case "P":
                return 100;
            case "N":
            case "B":
                return 300;
            case "R":
                return 500;
            case "Q":
                return 900;
            case "K":
                return 20000;
        }
        return 0;
    }

    private int centerBonus(Position pos) {
        int r = pos.getRow(), c = pos.getColumn();
        if ((r == 3 || r == 4) && (c == 3 || c == 4))
            return 10;
        if ((r >= 2 && r <= 5) && (c >= 2 && c <= 5))
            return 4;
        return 0;
    }

    private void refresh() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                Color base = light ? LIGHT_SQ : DARK_SQ;
                JButton b = squares[r][c];
                b.setBackground(base);
                b.setBorder(null);
                b.setToolTipText(null);
            }
        }

        if (lastFrom != null)
            squares[lastFrom.getRow()][lastFrom.getColumn()].setBorder(BORDER_LASTMOVE);
        if (lastTo != null)
            squares[lastTo.getRow()][lastTo.getColumn()].setBorder(BORDER_LASTMOVE);

        if (selected != null) {
            squares[selected.getRow()][selected.getColumn()].setBorder(BORDER_SELECTED);
            for (Position d : legalForSelected) {
                squares[d.getRow()][d.getColumn()].setBorder(BORDER_LEGAL);
            }
        }

        int iconSize = computeSquareIconSize();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.board().get(new Position(r, c));
                JButton b = squares[r][c];

                if (p == null) {
                    b.setIcon(null);
                    b.setText("");
                    continue;
                }

                char sym = p.getSymbol().charAt(0);
                ImageIcon icon = ImageUtil.getPieceIcon(p.isWhite(), sym, iconSize);
                if (icon != null) {
                    b.setIcon(icon);
                    b.setText("");
                } else {
                    b.setIcon(null);
                    b.setText(toUnicode(p.getSymbol(), p.isWhite()));
                }
            }
        }
        StringBuilder capturesText = new StringBuilder(" | Capturas: ");
        for (String symbol : capturedBlack) {
            capturesText.append(toUnicode(symbol, false)).append(" ");
        }
        for (String symbol : capturedWhite) {
            capturesText.append(toUnicode(symbol, true)).append(" ");
        }

        String side = game.whiteToMove() ? "Eu" : "IA";
        String chk = game.inCheck(game.whiteToMove()) ? " — Xeque!" : "";
        if (aiThinking)
            chk = " — PC pensando...";

        status.setText("Vez: " + side + chk + capturesText.toString());

        StringBuilder sb = new StringBuilder();
        var hist = game.history();
        for (int i = 0; i < hist.size(); i++) {
            if (i % 2 == 0)
                sb.append((i / 2) + 1).append('.').append(' ');
            sb.append(hist.get(i)).append(' ');
            if (i % 2 == 1)
                sb.append('\n');
        }
        history.setText(sb.toString());
        history.setCaretPosition(history.getDocument().getLength());
    }

    private void maybeAnnounceEnd() {
        if (!game.isGameOver())
            return;
        String msg;
        if (game.inCheck(game.whiteToMove())) {
            msg = "Xeque-Mate: Eu sou o melhor!! " + (game.whiteToMove() ? "Eu" : "IA")
                    + " estão em mate.";
        } else {
            msg = "Empate por afogamento (stalemate).";
        }
        JOptionPane.showMessageDialog(this, msg, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
    }

    private String toUnicode(String sym, boolean white) {
        return switch (sym) {
            case "K" -> white ? "\u2654" : "\u265A";
            case "Q" -> white ? "\u2655" : "\u265B";
            case "R" -> white ? "\u2656" : "\u265C";
            case "B" -> white ? "\u2657" : "\u265D";
            case "N" -> white ? "\u2658" : "\u265E";
            case "P" -> white ? "\u2659" : "\u265F";
            default -> "";
        };
    }

    private int computeSquareIconSize() {
        JButton b = squares[0][0];
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        int side = Math.min(w, h);
        if (side <= 1)
            return 64;
        return Math.max(24, side - 8);
    }

    private Icon loadPieceIcon(String key) {
        String resourcePath = "/resources/" + key + ".png";
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = ImageIO.read(url).getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
            String[] fallbacks = new String[] {
                    "resources/" + key + ".png",
                    "../resources/" + key + ".png",
                    "./resources/" + key + ".png"
            };
            for (String fp : fallbacks) {
                java.io.File f = new java.io.File(fp);
                if (f.exists()) {
                    Image img = ImageIO.read(f).getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}

