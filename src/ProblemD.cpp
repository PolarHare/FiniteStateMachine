#include <vector>
#include <cstdio>
#include <iostream>
#include <iterator>

using std::vector;
using std::cout;
using std::endl;

template<typename T>
void fillWith(T *array, int n, T value) {
    for (int i = 0; i < n; i++) {
        array[i] = value;
    }
}

template<typename T>
void fillWith(T **array, int n, int m, T value) {
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            array[i][j] = value;
        }
    }
}

template<typename T>
T **newArray(int n, int m) {
    T **a = new T *[n];
    for (int i = 0; i < n; ++i) {
        a[i] = new T[m];
    }
    return a;
}


const static int DEVIL_STATE = 0;
const static int START_STATE = 1;
const static char minChar = 'a';
const static char maxChar = 'z';
const static int charsCount = maxChar - minChar + 1;

struct Node {
    int value;
    Node *next = nullptr;
    Node *prev = nullptr;


    Node() : value(-1) {
    }

    Node(int value) : value(value) {
    }

    void insertAfter(Node *node) {
        prev = node;
        next = node->next;
        prev->next = this;
        if (next != nullptr) {
            next->prev = this;
        }
    }

    void takeOut() {
        prev->next = next;
        if (next != nullptr) {
            next->prev = prev;
        }
        prev = nullptr;
        next = nullptr;
    }

    ~Node() {
        cout << "DESTR" << endl;
    }
};

struct DKA {
    int n;
    int **to;
    bool *isFinish;

    DKA(int n) : n(n) {
        to = newArray<int>(n, charsCount);
        fillWith(to, n, charsCount, DEVIL_STATE);
        isFinish = new bool[n];
        fillWith(isFinish, n, false);
    }

    vector<int> **calcFrom() {
        vector<int> **from = newArray<vector<int>>(n, charsCount);
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < charsCount; c++) {
                from[to[i][c]][c].push_back(i);
            }
        }
        return from;
    }

    int countEdges() {
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

    int countFinishes() {
        int res = 0;
        for (int i = 1; i < n; i++) {
            if (isFinish[i]) {
                res++;
            }
        }
        return res;
    }

    bool *calcIsStartable(vector<int> **from) {
        bool *isStartable = new bool[n];
        fillWith(isStartable, n, false);

        int q[n];
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

    bool *calcIsReachable() {
        bool *isReachable = new bool[n];
        fillWith(isReachable, n, false);

        int q[n];
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

    DKA *removeNotReachables() {
        bool *isReachable = calcIsReachable();
        bool *isStartable = calcIsStartable(calcFrom());
        bool isOk[n];
        fillWith(isOk, n, false);
        int resN = 0;
        for (int i = 0; i < n; i++) {
            if (i == DEVIL_STATE || (isReachable[i] && isStartable[i])) {
                isOk[i] = true;
                resN++;
            }
        }
        DKA *dka = new DKA(resN);
        int newIdByOldId[n];
        fillWith(newIdByOldId, n, -1);
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
                dka->isFinish[newIdByOldId[i]] = true;
            }
            for (int c = 0; c < charsCount; c++) {
                int toS = to[i][c];
                if (!isOk[toS]) {
                    continue;
                }
                dka->to[newIdByOldId[i]][c] = newIdByOldId[toS];
            }
        }
        return dka;
    }

    DKA *minimize() {
        vector<int> **from = calcFrom();

        int classCount = 0;
        int classSize[n];
        fillWith(classSize, n, 0);
        int stateClassId[n];
        fillWith(stateClassId, n, -1);
        Node *stateClassNode[n];
        Node *classHead[n];

        for (int i = 0; i < n; i++) {
            classHead[i] = new Node();
        }

        bool **isInQ = newArray<bool>(n, charsCount);
        fillWith(isInQ, n, charsCount, false);
        int classQ[charsCount * n];
        int charQ[charsCount * n];
        int nextQ = 0;
        int lastQ = -1;

        {
            const int notFinishClassId = 0;
            const int finishClassId = 1;
            classCount += 2;
            for (int i = 0; i < n; i++) {
                int classId = isFinish[i] ? finishClassId : notFinishClassId;
                classSize[classId]++;
                stateClassId[i] = classId;
                stateClassNode[i] = new Node(i);
                stateClassNode[i]->insertAfter(classHead[classId]);
            }

            int minClassId = classSize[finishClassId] < classSize[notFinishClassId] ? finishClassId : notFinishClassId;
            for (int c = 0; c < charsCount; c++) {
                lastQ++;
                classQ[lastQ] = minClassId;
                charQ[lastQ] = c;
                isInQ[minClassId][c] = true;
            }
        }

        int involvedCount[n];
        int involvedTwinClass[n];
        fillWith(involvedTwinClass, n, -1);
        fillWith(involvedCount, n, 0);

        int involvedClasses[n];
        int involvedClassesLast;
        int fromSList[n * charsCount];
        int fromSListLast;
        while (nextQ <= lastQ) {
            int curClass = classQ[nextQ];
            int curChar = charQ[nextQ];
            nextQ++;

            involvedClassesLast = -1;
            fromSListLast = -1;

            Node *n = classHead[curClass]->next;
            while (n != nullptr) {
                int state = n->value;
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
                n = n->next;
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
                    Node *toMove = stateClassNode[fromS];
                    toMove->takeOut();
                    classSize[classId]--;

                    involvedCount[classId]--;

                    stateClassId[fromS] = newClass;
                    toMove->insertAfter(classHead[newClass]);
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

        int classIdToNewStateId[classCount];
        fillWith(classIdToNewStateId, classCount, -1);
        int nextId = 2;
        for (int i = 0; i < classCount; i++) {
            bool isStart = false;
            bool isDevil = false;
            Node *n = classHead[i]->next;
            while (n != nullptr) {
                if (n->value == START_STATE) {
                    isStart = true;
                } else if (n->value == DEVIL_STATE) {
                    isDevil = true;
                }
                n = n->next;
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

        DKA *min = new DKA(classCount);
        {
            for (int i = 0; i < classCount; i++) {
                int id = classIdToNewStateId[i];
                bool isFinishClass = false;
                Node *n = classHead[i]->next;
                while (n != nullptr) {
                    if (isFinish[n->value]) {
                        isFinishClass = true;
                    }

                    for (int c = 0; c < charsCount; c++) {
                        int toState = stateClassId[to[n->value][c]];
                        min->to[id][c] = classIdToNewStateId[toState];
                    }
                    n = n->next;
                }
                min->isFinish[id] = isFinishClass;
            }
        }
        return min;
    }

    bool isIsomorphTo(DKA *that) {
        if (n == 1 || that->n == 1) {
            return n == that->n;
        }
        int fromThisToThatIndex[n];
        fillWith(fromThisToThatIndex, n, -1);

        int q1[n];
        int q2[n];
        int next = 0;
        int last = 0;
        q1[last] = START_STATE;
        q2[last] = START_STATE;
        fromThisToThatIndex[START_STATE] = START_STATE;
        bool fail = false;
        while (next <= last && !fail) {
            int id1 = q1[next];
            int id2 = q2[next];
            next++;
            if (isFinish[id1] != that->isFinish[id2]) {
                fail = true;
                break;
            }
            for (int c = 0; c < charsCount; c++) {
                int to1 = to[id1][c];
                int to2 = that->to[id2][c];
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
};

int main() {
    FILE *in = fopen("fastminimization.in", "r");
    FILE *out = fopen("fastminimization.out", "w");

    int n;
    int m;
    int k;
    fscanf(in, "%d %d %d", &n, &m, &k);

    DKA *dka = new DKA(n + 1);

    for (int i = 0; i < k; ++i) {
        int ind;
        fscanf(in, "%d", &ind);
        dka->isFinish[ind] = true;
    }

    for (int i = 0; i < m; ++i) {
        int from;
        int to;
        char c;
        fscanf(in, "%d %d %c", &from, &to, &c);
        dka->to[from][c - minChar] = to;
    }

    DKA *minDka = dka->removeNotReachables();
    if (minDka->n > 1) {
        minDka = minDka->minimize();
    }

    fprintf(out, "%d %d %d\n", minDka->n - 1, minDka->countEdges(), minDka->countFinishes());
    if (minDka->countFinishes() > 0) {
        for (int i = 1; i < minDka->n; ++i) {
            if (minDka->isFinish[i]) {
                fprintf(out, "%d ", i);
            }
        }
        fprintf(out, "\n");
    }

    if (minDka->countEdges() > 0) {
        for (int i = 1; i < minDka->n; ++i) {
            for (int c = 0; c < charsCount; ++c) {
                if (minDka->to[i][c] != DEVIL_STATE) {
                    fprintf(out, "%d %d %c\n", i, minDka->to[i][c], minChar + c);
                }
            }
        }
    }

    fclose(out);
    fclose(in);
}