import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Polyarnyi Nikolay - PolarNick239
 */
public class ProblemD {

    final static int DEVIL_STATE = 0;
    final static int START_STATE = 1;
    final static char minChar = 'a';
    final static char maxChar = 'z';
    final static int charsCount = maxChar - minChar + 1;

    static class Node implements Iterable<Node> {
        int value;
        Node next = null;
        Node prev = null;

        public Node(int value) {
            this.value = value;
        }

        public void insertAfter(Node node) {
            this.prev = node;
            this.next = node.next;
            this.prev.next = this;
            if (this.next != null) {
                this.next.prev = this;
            }
        }

        public void takeOut() {
            this.prev.next = this.next;
            if (this.next != null) {
                this.next.prev = this.prev;
            }
            this.prev = null;
            this.next = null;
        }

        @Override
        public Iterator<Node> iterator() {
            return new Iterator<Node>() {
                Node next = Node.this.next;

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Node next() {
                    Node res = next;
                    next = next.next;
                    return res;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String toString() {
            String result = Integer.toString(value);
            for (Node n : this) {
                result += ", " + n.value;
            }
            return result;
        }
    }

    static class DKA {
        int n;
        int[][] to;
        boolean[] isFinish;

        public DKA(int n) {
            this.n = n;
            this.to = new int[n][charsCount];
            this.isFinish = new boolean[n];
            for (int i = 0; i < n; i++) {
                for (int c = 0; c < charsCount; c++) {
                    to[i][c] = DEVIL_STATE;
                }
            }
        }

        public List<Integer>[][] calcFrom() {
            List<Integer>[][] from = new List[n][charsCount];
            for (int i = 0; i < n; i++) {
                for (int c = 0; c < charsCount; c++) {
                    from[i][c] = new ArrayList<Integer>();
                }
            }

            for (int i = 0; i < n; i++) {
                for (int c = 0; c < charsCount; c++) {
                    from[to[i][c]][c].add(i);
                }
            }
            return from;
        }

        public int countEdges() {
            int res = 0;
            for (int i = 1; i < n; i++) {
                for (int c = 0; c < charsCount; ++c) {
                    if (to[i][c] != DEVIL_STATE) {
                        res++;
                    }
                }
            }
            return res;
        }

        public int countFinishes() {
            int res = 0;
            for (int i = 1; i < n; i++) {
                if (isFinish[i]) {
                    res++;
                }
            }
            return res;
        }

        public boolean[] calcIsStartable(List<Integer>[][] from) {
            boolean[] isStartable = new boolean[n];

            int[] q = new int[n];
            int next = 0;
            int last = -1;
            for (int i = 0; i < n; i++) {
                if (isFinish[i]) {
                    isStartable[i] = true;
                    last++;
                    q[last] = i;
                }
            }
            while (next <= last) {
                int cur = q[next];
                next++;
                for (int c = 0; c < charsCount; c++) {
                    for (int fromS : from[cur][c]) {
                        if (!isStartable[fromS]) {
                            isStartable[fromS] = true;
                            last++;
                            q[last] = fromS;
                        }
                    }
                }
            }
            return isStartable;
        }

        public boolean[] calcIsReachable() {
            boolean[] isReachable = new boolean[n];

            int[] q = new int[n];
            q[0] = START_STATE;
            isReachable[START_STATE] = true;
            int next = 0;
            int last = 0;
            while (next <= last) {
                int cur = q[next];
                next++;
                for (int c = 0; c < charsCount; c++) {
                    int toV = to[cur][c];
                    if (!isReachable[toV]) {
                        isReachable[toV] = true;
                        last++;
                        q[last] = toV;
                    }
                }
            }
            return isReachable;
        }

        public DKA removeNotReachables() {
            boolean[] isReachable = calcIsReachable();
            boolean[] isStartable = calcIsStartable(calcFrom());
            boolean[] isOk = new boolean[n];
            int resN = 0;
            for (int i = 0; i < n; i++) {
                if (i == DEVIL_STATE || (isReachable[i] && isStartable[i])) {
                    isOk[i] = true;
                    resN++;
                }
            }
            DKA dka = new DKA(resN);
            int[] newIdByOldId = new int[n];
            Arrays.fill(newIdByOldId, -1);
            newIdByOldId[DEVIL_STATE] = DEVIL_STATE;
            int nextId = 1;
            for (int i = 1; i < n; i++) {
                if (isOk[i]) {
                    newIdByOldId[i] = nextId;
                    nextId++;
                }
            }
            for (int i = 0; i < n; i++) {
                if (!isOk[i]) {
                    continue;
                }
                if (isFinish[i]) {
                    dka.isFinish[newIdByOldId[i]] = true;
                }
                for (int c = 0; c < charsCount; c++) {
                    int toS = to[i][c];
                    if (!isOk[toS]) {
                        continue;
                    }
                    dka.to[newIdByOldId[i]][c] = newIdByOldId[toS];
                }
            }
            return dka;
        }

        public DKA minimize() {
            List<Integer>[][] from = calcFrom();

            int classCount = 0;
            int[] classSize = new int[n];
            int[] stateClassId = new int[n];
            Node[] stateClassNode = new Node[n];
            Node[] classHead = new Node[n];
            for (int i = 0; i < n; i++) {
                classHead[i] = new Node(-1);
            }

            boolean[][] isInQ = new boolean[n][charsCount];
            int[] classQ = new int[charsCount * n];
            int[] charQ = new int[charsCount * n];
            int nextQ = 0;
            int lastQ = -1;

            {
                final int notFinishClassId = 0;
                final int finishClassId = 1;
                classCount += 2;
                for (int i = 0; i < n; i++) {
                    int classId = isFinish[i] ? finishClassId : notFinishClassId;
                    classSize[classId]++;
                    stateClassId[i] = classId;
                    stateClassNode[i] = new Node(i);
                    stateClassNode[i].insertAfter(classHead[classId]);
                }

                int minClassId = classSize[finishClassId] < classSize[notFinishClassId] ? finishClassId : notFinishClassId;
                for (int c = 0; c < charsCount; c++) {
                    lastQ++;
                    classQ[lastQ] = minClassId;
                    charQ[lastQ] = c;
                    isInQ[minClassId][c] = true;
                }
            }

            int[] involvedCount = new int[n];
            int[] involvedTwinClass = new int[n];
            Arrays.fill(involvedTwinClass, -1);

            int[] involvedClasses = new int[n];
            int involvedClassesLast;
            int[] fromSList = new int[n * charsCount];
            int fromSListLast;
            while (nextQ <= lastQ) {
                int curClass = classQ[nextQ];
                int curChar = charQ[nextQ];
                nextQ++;

                involvedClassesLast = -1;
                fromSListLast = -1;
                for (Node n : classHead[curClass]) {
                    int state = n.value;
                    for (int fromS : from[state][curChar]) {
                        fromSListLast++;
                        fromSList[fromSListLast] = fromS;
                        int classId = stateClassId[fromS];
                        if (involvedCount[classId] == 0) {
                            involvedClassesLast++;
                            involvedClasses[involvedClassesLast] = classId;
                        }
                        involvedCount[classId]++;
                    }
                }
                for (int i = 0; i <= fromSListLast; i++) {
                    int fromS = fromSList[i];
                    int classId = stateClassId[fromS];
                    if (involvedCount[classId] < classSize[classId]) {
                        if (involvedTwinClass[classId] == -1) {
                            involvedTwinClass[classId] = classCount;
                            classCount++;
                        }

                        int newClass = involvedTwinClass[classId];
                        Node toMove = stateClassNode[fromS];
                        toMove.takeOut();
                        classSize[classId]--;

                        involvedCount[classId]--;

                        stateClassId[fromS] = newClass;
                        toMove.insertAfter(classHead[newClass]);
                        classSize[newClass]++;
                    }
                }

                for (int i = 0; i <= involvedClassesLast; i++) {
                    int classId = involvedClasses[i];
                    if (involvedCount[classId] < classSize[classId]) {
                        for (int c = 0; c < charsCount; c++) {
                            int classToAdd = -1;
                            if (isInQ[classId][c]) {
                                classToAdd = involvedTwinClass[classId];
                            } else {
                                if (classSize[involvedTwinClass[classId]] < classSize[classId]) {
                                    classToAdd = involvedTwinClass[classId];
                                } else {
                                    classToAdd = classId;
                                }
                            }
                            lastQ++;
                            classQ[lastQ] = classToAdd;
                            charQ[lastQ] = c;
                            isInQ[classToAdd][c] = true;
                        }
                    }
                    involvedCount[classId] = 0;
                    involvedTwinClass[classId] = -1;
                }
            }

            int[] classIdToNewStateId = new int[classCount];
            Arrays.fill(classIdToNewStateId, -1);
            int nextId = 2;
            for (int i = 0; i < classCount; i++) {
                boolean isStart = false;
                boolean isDevil = false;
                for (Node n : classHead[i]) {
                    if (n.value == START_STATE) {
                        isStart = true;
                    } else if (n.value == DEVIL_STATE) {
                        isDevil = true;
                    }
                }
                if (isStart) {
                    classIdToNewStateId[i] = START_STATE;
                } else if (isDevil) {
                    classIdToNewStateId[i] = DEVIL_STATE;
                } else {
                    classIdToNewStateId[i] = nextId;
                    nextId++;
                }
            }

            DKA min = new DKA(classCount);
            {
                for (int i = 0; i < classCount; i++) {
                    int id = classIdToNewStateId[i];
                    boolean isFinishClass = false;
                    for (Node n : classHead[i]) {
                        if (isFinish[n.value]) {
                            isFinishClass = true;
                        }

                        for (int c = 0; c < charsCount; c++) {
                            int toState = stateClassId[to[n.value][c]];
                            min.to[id][c] = classIdToNewStateId[toState];
                        }
                    }
                    min.isFinish[id] = isFinishClass;
                }
            }
            return min;
        }

        public boolean isIsomorphTo(DKA that) {
            if (n == 1 || that.n == 1) {
                return n == that.n;
            }
            int[] fromThisToThatIndex = new int[n];
            Arrays.fill(fromThisToThatIndex, -1);

            int[] q1 = new int[n];
            int[] q2 = new int[n];
            int next = 0;
            int last = 0;
            q1[last] = START_STATE;
            q2[last] = START_STATE;
            fromThisToThatIndex[START_STATE] = START_STATE;
            boolean fail = false;
            while (next <= last && !fail) {
                int id1 = q1[next];
                int id2 = q2[next];
                next++;
                if (isFinish[id1] != that.isFinish[id2]) {
                    fail = true;
                    break;
                }
                for (int c = 0; c < charsCount; c++) {
                    int to1 = to[id1][c];
                    int to2 = that.to[id2][c];
                    if (fromThisToThatIndex[to1] == -1) {
                        fromThisToThatIndex[to1] = to2;
                        last++;
                        q1[last] = to1;
                        q2[last] = to2;
                    } else if (fromThisToThatIndex[to1] != to2) {
                        fail = true;
                        break;
                    }
                }
            }
            return !fail;
        }

        @Override
        public String toString() {
            String result = (n - 1) + " " + countEdges() + " " + countFinishes() + "\n";
            for (int i = 0; i < n; i++) {
                if (isFinish[i]) {
                    result += i + " ";
                }
            }
            result += "\n";
            for (int i = 0; i < n; i++) {
                for (int c = 0; c < charsCount; c++) {
                    if (to[i][c] != DEVIL_STATE) {
                        result += i + " " + to[i][c] + " " + (char) (minChar + c) + "\n";
                    }
                }
            }
            return result;
        }
    }

    private static DKA readDka(BufferedReader in) throws IOException {
        StringTokenizer tok = new StringTokenizer(in.readLine());
        DKA dka = new DKA(Integer.parseInt(tok.nextToken()) + 1);
        {
            int m = Integer.parseInt(tok.nextToken());
            int k = Integer.parseInt(tok.nextToken());

            tok = new StringTokenizer(in.readLine());
            for (int i = 0; i < k; i++) {
                dka.isFinish[Integer.parseInt(tok.nextToken())] = true;
            }

            for (int i = 0; i < m; i++) {
                tok = new StringTokenizer(in.readLine());
                int from = Integer.parseInt(tok.nextToken());
                int to = Integer.parseInt(tok.nextToken());
                int c = tok.nextToken().charAt(0) - minChar;
                dka.to[from][c] = to;
            }
        }
        return dka;
    }

    public static void shuffleArray(int[] a, Random r) {
        for (int i = 0; i < a.length; i++) {
            int j = r.nextInt(a.length);
            int tmp = a[j];
            a[j] = a[i];
            a[i] = tmp;
        }
    }

    static DKA generateDKA(Random r, int n, int m) {
        int k = 1 + r.nextInt(n);
        DKA dka = new DKA(n + 1);

        int[] allIndexes = new int[n];
        for (int i = 0; i < n; i++) {
            allIndexes[i] = i + 1;
        }
        shuffleArray(allIndexes, r);
        for (int i = 0; i < k; i++) {
            dka.isFinish[allIndexes[i]] = true;
        }

        for (int i = 0; i < m; i++) {
            int from = 1 + r.nextInt(n);
            int to = 1 + r.nextInt(n);
            int c = r.nextInt(charsCount);
            while (dka.to[from][c] != DEVIL_STATE) {
                from = 1 + r.nextInt(n);
                to = 1 + r.nextInt(n);
                c = r.nextInt(charsCount);
            }
            dka.to[from][c] = to;
        }
        if (dka.countEdges() != m || dka.countFinishes() != k) {
            throw new IllegalStateException();
        }
        return dka;
    }

    public static void stressTest() {
        Random seedGenerator = new Random(239);
        int skipIters = 10;
        long maxTime = -1;
        int maxSeed = -1;
        while (true) {
            long start = System.currentTimeMillis();
            int seed = seedGenerator.nextInt();
            Random r = new Random(seed);
            System.out.println("Seed: " + seed);

            DKA dka = generateDKA(r, 50_000, 100_000);
            dka = dka.removeNotReachables();
            if (dka.n - 1 >= 1) {
                dka = dka.minimize();
            }
            System.out.println(" n=" + (dka.n - 1) + ", m=" + dka.countEdges() + ", k=" + dka.countFinishes());

            long timePassed = System.currentTimeMillis() - start;
            System.out.println("  time: " + timePassed + " ms");

            if (skipIters != 0) {
                skipIters--;
                continue;
            }

            if (timePassed > maxTime) {
                maxTime = timePassed;
                maxSeed = seed;
            }
            System.out.println("  (maxTime: " + maxTime + " ms with seed: " + maxSeed + ")");
        }
    }

    public static void maxTest() {
        int n = 50_000;
        int m = n - 1;
        int k = 1;
        long start = System.currentTimeMillis();

        DKA dka = new DKA(n + 1);

        dka.isFinish[n] = true;

        for (int i = 1; i <= n - 1; i++) {
            dka.to[i][i % charsCount] = i + 1;
        }

        dka = dka.removeNotReachables();
        if (dka.n - 1 >= 1) {
            dka = dka.minimize();
        }
        System.out.println("Stress time: " + (System.currentTimeMillis() - start) + " ms");
    }

    public static void main(String[] args) throws Exception {
//        Stress time: 1270 ms -> 807 ms
//        Stress time: 568 ms  -> 608 ms
//        Stress time: 413 ms  -> 149 ms
//        Stress time: 340 ms  -> 135 ms
//        Stress time: 237 ms  -> 121 ms
//        Stress time: 260 ms  -> 178 ms
//        Stress time: 241 ms  -> 88 ms
//        Stress time: 199 ms  -> 162 ms
//        Stress time: 211 ms  -> 86 ms
//        Stress time: 235 ms  -> 105 ms
//        stressTest();
//        for (int i = 0; i < 10; i++) {
//            maxTest();
//        }

        BufferedReader in = new BufferedReader(new FileReader("fastminimization.in"));
        DKA dka = readDka(in);

        dka = dka.removeNotReachables();
        if (dka.n - 1 >= 1) {
            dka = dka.minimize();
        }

        PrintWriter out = new PrintWriter("fastminimization.out");

        out.println((dka.n - 1) + " " + dka.countEdges() + " " + dka.countFinishes());
        for (int i = 1; i < dka.n; i++) {
            if (dka.isFinish[i]) {
                out.print(i + " ");
            }
        }
        out.println();
        for (int i = 1; i < dka.n; i++) {
            for (int c = 0; c < charsCount; ++c) {
                if (dka.to[i][c] != DEVIL_STATE) {
                    out.println(i + " " + dka.to[i][c] + " " + (char) (minChar + c));
                }
            }
        }

        out.close();
    }

}
