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

public class Maze {

    public static final int MAX_HEIGHT = 2001;
    public static final int MAX_WIDTH = 2001;
    public static final int MAX_SIMPLICITY = 100;
    public static final char WALL = '#';
    public static final char ROAD = '.';
    public static final char START = 'S';
    public static final char GOAL = 'G';

    private static final char TEMP_STOPPER = '!';
    private static final int INF = Integer.MAX_VALUE;

    public static void main(String args[]) {

        Scanner scn = new Scanner(System.in);
        PrintWriter cout = new PrintWriter(System.out);

        int height = scn.nextInt();
        int width = scn.nextInt();
        int simplicity = scn.nextInt();
        int circuits = scn.nextInt();
        boolean debug = Objects.equals("-d", scn.next());

        char[][] maze = make(height, width, simplicity, circuits);

        if (debug) {
            cout.println("make:");
            for (char[] line : maze)
                cout.println(String.valueOf(line));
        }

        shortestPath(maze);

        if (debug) {
            cout.println("shortestPath:");
            for (char[] line : maze)
                cout.println(String.valueOf(line));
        }

        cout.flush();
    }

    public static char[][] make(int height, int width, int simplicity, int circuits) {
        final int h = height; // 縦幅
        final int w = width; // 横幅

        if (3 > h || h > MAX_HEIGHT || h % 2 == 0) {
            System.out.println("3 <= height <= " + MAX_HEIGHT + " の奇数のみ入力可能です");
            return new char[0][0];
        }
        if (3 > w || w > MAX_WIDTH || w % 2 == 0) {
            System.out.println("3 <= width <= " + MAX_WIDTH + " の奇数のみ入力可能です");
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

        // 上下左右
        int[] dy = { 0, -1, 0, 1 };
        int[] dx = { -1, 0, 1, 0 };
        int dn = dx.length;

        // 乱数
        Random rand = new Random(new Date().getTime());

        // （１）壁で埋め尽くします
        for (char[] line : maze)
            Arrays.fill(line, WALL);
        Arrays.fill(maze[0], TEMP_STOPPER);
        Arrays.fill(maze[h - 1], TEMP_STOPPER);
        for (int i = 0; i < h; i++) {
            maze[i][0] = TEMP_STOPPER;
            maze[i][w - 1] = TEMP_STOPPER;
        }

        // 掘る開始地点リスト
        // （初期サイズh*w/32は適当。最大サイズ < h*w/4なのは間違いない）
        PriorityQueue<int[]> q = new PriorityQueue<>(Math.max(1, h * w / 32),
                (arr1, arr2) -> Integer.compare(arr1[2], arr2[2]));

        // （２）真ん中辺りの奇数マスに道を開けます
        int[] stt = { h / 4 * 2 + 1, w / 4 * 2 + 1, 0 };
        maze[stt[0]][stt[1]] = ROAD;

        // （３）真ん中を掘る開始地点リストに追加します
        q.add(stt);
        while (q.isEmpty() == false) {

            // （４）掘る開始地点リストの先頭を取得します
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

                // よし掘れる、掘ろう
                maze[ny1][nx1] = ROAD;
                maze[ny2][nx2] = ROAD;

                // 厳密に言うと「掘る開始地点リスト」はリストでなく優先度キューです
                // 中身が常時ソートされた状態になっており、指定した順番で処理するのに効率がいいです
                int priority;
                if (rand.nextInt(MAX_SIMPLICITY) < simplicity) {
                    // 今回に掘った道の続きを最優先で掘ります
                    priority = -1;
                } else {
                    // p[2]の小さいやつから処理します
                    // ＝今までに掘った道から適当に選んで次の道を掘ります
                    priority = rand.nextInt(INF);
                }

                // 掘った先を掘る開始地点リストに追加します
                q.add(new int[] { ny2, nx2, priority });
                // 現在地点もリストに戻します
                // （１方向に掘ったら他の方向を掘らずに終わるので、戻しておかないと掘り漏れが発生します。
                // 全方向を掘り終わっていたら単に空振りします）
                q.add(new int[] { p[0], p[1], rand.nextInt(INF) });
                // 他の方向は掘らずに終わります
                break;
            }
        }
        // （６）掘る開始地点リストが空になったら終わりです、お疲れ様

        // （７）仕上げ１：外周をただの壁に戻します
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

        // （８）仕上げ２：適当な道に開始地点と終了地点を置きます
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

        // （９）仕上げ３：適当な壁に道を開けます
        // （作成直後の迷路には閉路＝回り道がありません）
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

    public static void shortestPath(char[][] maze) {
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

        // （２）開始地点をキューに入れます

        // アルゴリズムの世界でキューとはFIFO（先入先出法）を指し、
        // それ以外はキューと呼びません
        // つまり出す時はq.pollFirst()の位置のデータを出し、
        // 入れる時はq.addLast()の位置に入れます
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(s);
        vis[s[0]][s[1]] = 0;

        outer: while (q.isEmpty() == false) {

            // （３）キューから取得します
            int[] p = q.pollFirst();
            int y = p[0];
            int x = p[1];
            int m = p[2];
            vis[y][x] = m;
            if (y == g[0] && x == g[1]) {
                // （４）ゴールしてたら終了。
                break outer;
            }

            // （５）分身の術！
            for (int d = 0; d < dn; d++) {
                // 隣のマスが
                int ny = y + dy[d];
                int nx = x + dx[d];
                if (vis[ny][nx] != -1)
                    // 未計算でなければ別の隣のマスを探します
                    continue;
                // 未計算の場合＝行ったことない道かゴールの場合
                int[] np = new int[] { ny, nx, 1 + m };
                // （６）ここに分身を置いておこう
                q.addLast(np);
            }
        }
        q.clear();

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
        // お疲れ様
    }

}
