import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Polyarnyi Nikolay - PolarNick239
 */
public class ProblemC {

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
            while (nextQ <= lastQ) {
                int curClass = classQ[nextQ];
                int curChar = charQ[nextQ];
                nextQ++;

                List<Integer> involvedClasses = new ArrayList<>();
                List<Integer> fromSList = new ArrayList<>();
                for (Node n : classHead[curClass]) {
                    int state = n.value;
                    for (int fromS : from[state][curChar]) {
                        fromSList.add(fromS);
                        int classId = stateClassId[fromS];
                        if (involvedCount[classId] == 0) {
                            involvedClasses.add(classId);
                        }
                        involvedCount[classId]++;
                    }
                }
                for (int fromS : fromSList) {
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

                for (int classId : involvedClasses) {
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

    public static void shuffleArray(int[] a, Random r) {
        for (int i = 0; i < a.length; i++) {
            int j = r.nextInt(a.length);
            int tmp = a[j];
            a[j] = a[i];
            a[i] = tmp;
        }
    }

    public static void stressTest() {
        Random seedGenerator = new Random(239);
        while (true) {
            int seed = seedGenerator.nextInt();
            Random r = new Random(seed);

            int n = 1 + r.nextInt(50000);
            int m = 1 + r.nextInt(Math.min(50000, n * charsCount));
            int k = 1 + r.nextInt(n);

            System.out.println("Seed=" + seed + ", n=" + n);
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
//                System.out.println(" " + from + "->" + to + " (" + (char) (minChar + c) + ")");
            }

            dka = dka.removeNotReachables();
            if (dka.n == 0 || dka.countEdges() == 0 || dka.countFinishes() == 0) {
                System.out.println("skipped...");
                continue;
            }
            dka = dka.minimize();
        }
    }

    public static void main(String[] args) throws Exception {
//        stressTest();

        BufferedReader in = new BufferedReader(new FileReader("minimization.in"));
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

//        System.out.println("Input:\n" + dka);
//        DKA normalized = dka.removeNotReachables();
//        System.out.println("\nNormalized:\n" + normalized);
//        DKA minDka = normalized.minimize();
//        System.out.println("\nMinimized:\n" + minDka);
        dka = dka.removeNotReachables();
        dka = dka.minimize();

        PrintWriter out = new PrintWriter("minimization.out");
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
