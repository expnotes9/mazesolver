/*
MIT License

Copyright (c) 2019 Shusuke Kusao

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
// run on java 1.8
package com.shoek.mazesolver;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

public class Maze {

    public static final int MAX_SIMPLICITY = 100;
    public static final char WALL = '#';
    public static final char ROAD = '.';
    public static final char START = 'S';
    public static final char GOAL = 'G';

    private static final char TEMP_STOPPER = '!';
    private static final int INF = Integer.MAX_VALUE;

    public static void main(String args[]) {
        // 標準入力例
        // 迷路作る
        // 11 13 0 0 -make -debug
        // 作って解く
        // 5 15 0 0 -bfs -debug
        // 一本道マシマシ
        // 5 15 100 0 -bfs -debug
        // 回廊がないじゃん->適当に壁を掘る
        // 5 21 0 20 -bfs -debug
        // 最短経路でなく到達可能か調べるやつ
        // 5 21 0 20 -dfs -debug

        // 上限調査用
        // 2501 2501 50 1000 -make -no-debug
        // 2501 2501 50 1000 -bfs -no-debug
        // 2501 2501 50 1000 -dfs -no-debug

        Scanner scn = new Scanner(System.in);
        PrintWriter cout = new PrintWriter(System.out);

        int height = scn.nextInt();
        int width = scn.nextInt();
        int simplicity = scn.nextInt();
        int circuits = scn.nextInt();
        String proc = scn.next();
        boolean debug = Objects.equals("-debug", scn.next());

        BiConsumer<int[], ArrayDeque<int[]>> addDeq = null;
        if (Objects.equals(proc, "-bfs")) {
            addDeq = ADD_BFS;
        } else if (Objects.equals(proc, "-dfs")) {
            addDeq = ADD_DFS;
        } else {
            // makeのみ。
        }

        // make:迷路を作ります
        char[][] maze = make(height, width, simplicity, circuits);
        if (debug) {
            cout.println("make:");
            for (char[] line : maze)
                cout.println(String.valueOf(line));
        }

        if (addDeq == null) {
            // makeのみ。
        } else {

            // bfs/dfs:迷路を解きます
            xfs(maze, addDeq);
            if (debug) {
                cout.println(proc + ":");
                for (char[] line : maze)
                    cout.println(String.valueOf(line));
            }

        }

        cout.flush();
    }

    public static char[][] make(int height, int width, int simplicity, int circuits) {
        final int h = height; // 縦幅
        final int w = width; // 横幅

        if (3 > h || h % 2 == 0) {
            System.out.println("3 <= height の奇数のみ入力可能です");
            return new char[0][0];
        }
        if (3 > w || w % 2 == 0) {
            System.out.println("3 <= width の奇数のみ入力可能です");
            return new char[0][0];
        }
        if (h * w == 9) {
            System.out.println("開始地点と終了地点を置けません（周囲１マスは外周で、壁になります）");
            return new char[0][0];
        }
        if (0 > simplicity || simplicity > MAX_SIMPLICITY) {
            System.out.println("0 <= simplicity <= " + MAX_SIMPLICITY + " のみ入力可能です");
            return new char[0][0];
        }
        if (0 > circuits) {
            System.out.println("0 <= circuits のみ入力可能です");
            return new char[0][0];
        }

        // 戻り値（迷路）
        char[][] maze = new char[h][w];

        // （１）壁で埋め尽くします
        for (char[] line : maze)
            Arrays.fill(line, WALL);
        // 外周は一時的に専用文字に書き換えます（番兵法）
        Arrays.fill(maze[0], TEMP_STOPPER);
        Arrays.fill(maze[h - 1], TEMP_STOPPER);
        for (int i = 0; i < h; i++) {
            maze[i][0] = TEMP_STOPPER;
            maze[i][w - 1] = TEMP_STOPPER;
        }

        // 上下左右
        int[] dy = { 0, -1, 0, 1 };
        int[] dx = { -1, 0, 1, 0 };
        int dn = dx.length;

        // 乱数
        Random rand = new Random(new Date().getTime());

        // 到達済み地点リスト
        PriorityQueue<int[]> q = new PriorityQueue<>((arr1, arr2) -> Integer.compare(arr1[2], arr2[2]));

        // （２）真ん中辺りの奇数マスに道を開けます
        int[] stt = { h / 4 * 2 + 1, w / 4 * 2 + 1, 0 };
        maze[stt[0]][stt[1]] = ROAD;

        // （３）真ん中を到達済地点リストに追加します
        q.add(stt);
        while (q.isEmpty() == false) {

            // （４）到達済地点リストの先頭を取り出します
            int[] p = q.poll();
            int y = p[0];
            int x = p[1];

            // （５）上下左右から適当に選んで掘ります
            int drand = rand.nextInt(dn);
            for (int d = 0; d < dn; d++) {
                int dir = (drand + d) % dn;
                // 隣のマス
                int ny1 = y + dy[dir];
                int nx1 = x + dx[dir];
                if (maze[ny1][nx1] == TEMP_STOPPER)
                    // 外周なので掘れません
                    continue;
                // 隣の隣のマス
                int ny2 = ny1 + dy[dir];
                int nx2 = nx1 + dx[dir];
                if (maze[ny2][nx2] == ROAD)
                    // 別の経路で既に掘ってます
                    continue;

                // （６）よし掘れる、掘ろう
                maze[ny1][nx1] = ROAD;
                maze[ny2][nx2] = ROAD;

                // （７）simplicityパーセントで続きを掘ります
                // (100 - simplicity)パーセントでランダム順に掘る予約をします
                int priority;
                if (simplicity == 0) {
                    priority = rand.nextInt(INF);
                } else if (simplicity == MAX_SIMPLICITY) {
                    priority = -1;
                } else if (rand.nextInt(MAX_SIMPLICITY) < simplicity) {
                    priority = -1;
                } else {
                    priority = rand.nextInt(INF);
                }

                // （７）掘った先を到達済地点リストに追加します
                q.add(new int[] { ny2, nx2, priority });

                // 現在地点もリストに戻します
                // （１方向に掘ったら他の方向を掘らずに終わるので、戻しておかないと掘り漏れが発生します。全方向を掘り終わっていたら単に空振りします）
                q.add(new int[] { p[0], p[1], rand.nextInt(INF) });
                // 他の方向は掘らずに終わります
                break;
            }
        }
        // （８）到達済地点リストが空になったら掘り終わりです

        // （９）仕上げ１：外周をただの壁に戻します
        Arrays.fill(maze[0], WALL);
        Arrays.fill(maze[h - 1], WALL);
        for (int i = 0; i < h; i++) {
            maze[i][0] = WALL;
            maze[i][w - 1] = WALL;
        }

        // 壁、道カウント
        int roadsCount = 0;
        int wallsCount = 0;
        for (int j = h - 2; j >= 1; j--) {
            char[] line = maze[j];
            for (int i = line.length - 2; i >= 1; i--) {
                if (line[i] == TEMP_STOPPER)
                    line[i] = WALL;
                if (line[i] == ROAD)
                    roadsCount += 1;
                if (line[i] == WALL)
                    wallsCount += 1;
            }
        }

        // （１０）仕上げ２：適当な道に開始地点と終了地点を置きます
        int startPoint = rand.nextInt(roadsCount);
        int endPoint = rand.nextInt(roadsCount);
        while (startPoint == endPoint)
            endPoint = rand.nextInt(roadsCount);
        int road = 0;
        for (int j = h - 2; j >= 1; j--) {
            char[] line = maze[j];
            for (int i = line.length - 2; i >= 1; i--) {
                if (line[i] != ROAD)
                    continue;
                if (road == startPoint)
                    line[i] = START;
                if (road == endPoint)
                    line[i] = GOAL;
                road += 1;
            }
        }

        // （１１）仕上げ３：circuitsの指定だけ、適当に壁を掘ります
        // 作成直後の迷路はグラフ理論で言う木です。閉路＝回り道がありません。
        // そのままだと退屈なので適当に回り道を作ります
        // 重複ありです。入力とリアルラック次第では同じ位置を２回掘ろうとします
        Set<Integer> c = new HashSet<>(circuits);
        for (int i = 0; i < circuits; i++)
            c.add(rand.nextInt(wallsCount));
        int wall = 0;
        for (int j = h - 2; j >= 1; j--) {
            char[] line = maze[j];
            for (int i = line.length - 2; i >= 1; i--) {
                if (line[i] != WALL)
                    continue;
                if (c.contains(wall))
                    line[i] = ROAD;
                wall += 1;
            }
        }

        // ありがとうございました
        return maze;
    }

    private static BiConsumer<int[], ArrayDeque<int[]>> ADD_BFS = (point, deq) -> deq.addFirst(point);

    private static BiConsumer<int[], ArrayDeque<int[]>> ADD_DFS = (point, deq) -> deq.addLast(point);

    public static void xfs(char[][] maze, BiConsumer<int[], ArrayDeque<int[]>> addDeq) {
        // 最短経路を探してmazeを書き換えます（破壊的動作）

        final int h = maze.length; // 縦幅
        final int w = maze[0].length; // 横幅

        // 上下左右
        int[] dy = { 0, -1, 0, 1 };
        int[] dx = { -1, 0, 1, 0 };
        int dn = dx.length;

        // 最短距離メモvis[i][j] = 開始地点からmaze[i][j]に到達するまでの最短距離
        int[][] vis = new int[h][w];
        for (int[] v : vis)
            Arrays.fill(v, -1); // 全部、未計算

        int[] s = null;
        int[] g = null;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                switch (maze[i][j]) {
                case START:
                    // （１）開始地点を検出します、３つ目の数は開始地点からの最短距離です
                    s = new int[] { i, j, 0 };
                    break;
                case GOAL:
                    // （２）終了地点も検出します、ついでに一時的に道扱いします
                    maze[i][j] = ROAD;
                    g = new int[] { i, j };
                    break;
                case WALL:
                    // （３）壁マスは到達不可能（超長距離）としておきます
                    vis[i][j] = INF;
                    break;
                default:
                }
            }
        }
        if (s == null || g == null) {
            throw new IllegalArgumentException("開始地点" + START + "または終了地点" + GOAL + "がありません");
        }

        // （２）開始地点をコレクション（キューまたはスタック）に入れます
        ArrayDeque<int[]> deq = new ArrayDeque<>();
        deq.add(s);
        vis[s[0]][s[1]] = 0;

        while (deq.isEmpty() == false) {

            // （３）コレクションから取り出します
            int[] p = deq.pollLast();
            int y = p[0];
            int x = p[1];
            int m = p[2];
            vis[y][x] = m;
            if (y == g[0] && x == g[1]) {
                // （４）ゴールしてたら終了。
                break;
            }

            // （５）上下左右に移動してみます
            for (int d = 0; d < dn; d++) {
                // 隣のマスが
                int ny = y + dy[d];
                int nx = x + dx[d];
                if (vis[ny][nx] != -1)
                    // 未計算でなければ別の隣のマスを探します
                    continue;
                // 未計算の場合＝行ったことない道かゴールの場合
                int[] np = new int[] { ny, nx, 1 + m };
                // （６）分身の術！
                // コレクションに分身を入れておきます
                // 前述の取り出しがpollLastであることに注意。
                // BFSは取り出し時と逆に入れます（FIFO：先入先出法）
                // DFSは取り出し時と同じに入れます（LIFO：先入先出法）
                addDeq.accept(np, deq);
            }
        }
        deq.clear();
        if (vis[g[0]][g[1]] == -1) {
            throw new IllegalArgumentException("終了地点に到達できませんでした");
        }

        // 最短距離をmazeに書き込みましょう

        // （７）終了地点から初めて、開始地点に戻るまで
        int[] p = Arrays.copyOf(g, g.length);
        while (p[0] != s[0] || p[1] != s[1]) {
            int y = p[0];
            int x = p[1];
            for (int d = 0; d < dn; d++) {
                int ny = y + dy[d];
                int nx = x + dx[d];
                if (vis[ny][nx] + 1 != vis[y][x])
                    continue;
                // （８）最短距離の1桁目、0~9を書きこんで
                maze[y][x] = (char) (vis[y][x] % 10 + '0');
                // （９）一歩戻ります
                p[0] = ny;
                p[1] = nx;
                break;
            }
        }

        // 終了地点をマークに戻して
        maze[g[0]][g[1]] = GOAL;
        // 〜終わり〜
    }

}
