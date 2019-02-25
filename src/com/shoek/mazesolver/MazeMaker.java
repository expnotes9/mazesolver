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

public class MazeMaker {

    static class FastScanner {
        private BufferedReader br;
        private StringTokenizer st;

        FastScanner() {
            br = new BufferedReader(new InputStreamReader(System.in));
        }

        String next() {
            while (st == null || !st.hasMoreElements())
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }
    }

    public static final int MAX_HEIGHT = 1001;
    public static final int MAX_WIDTH = 1001;
    public static final int MAX_SIMPLICITY = 100;
    public static final char WALL = '#';
    public static final char ROAD = '.';
    public static final char START = 'S';
    public static final char GOAL = 'G';

    private static final char TEMP_STOPPER = '!';
    private static final int INF = Integer.MAX_VALUE;

    public static void main(String args[]) {
        FastScanner cin = new FastScanner();
        PrintWriter cout = new PrintWriter(System.out);

        int h = cin.nextInt();
        int w = cin.nextInt();
        int s = cin.nextInt();
        int c = cin.nextInt();

        try {

            char[][] maze = make(h, w, s, c);

            for (char[] line : maze)
                cout.println(String.valueOf(line));

        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String st = sw.toString();
            cout.println(st);
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
        PriorityQueue<int[]> q = new PriorityQueue<>(h * w / 32, (arr1, arr2) -> Integer.compare(arr1[2], arr2[2]));

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

}
