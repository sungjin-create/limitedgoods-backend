import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

import { SharedArray } from 'k6/data';

const BASE_URL = 'http://localhost:8080';
const PRODUCT_ID = 16;        // 테스트할 상품 ID
const USER_COUNT = 1000;     // 생성할 가상 유저 수

export const options = {
    setupTimeout: '10m',         // setup() 최대 실행 시간
    scenarios: {
        queue_test: {
            executor: 'shared-iterations',
            vus: 200,            // 동시 접속 VU 수
            iterations: USER_COUNT,
            maxDuration: '10m',
        },
    },
};

// ─────────────────────────────────────────
// setup(): 테스트 시작 전 1회 실행
// 1000명 회원가입 → 로그인 → 토큰 수집 (batch 병렬 처리)
// ─────────────────────────────────────────
export function setup() {
    const headers    = { 'Content-Type': 'application/json' };
    const password   = 'Test1234!';
    const BATCH_SIZE = 100;  // 한 번에 처리할 요청 수 (서버 부하 조절)

    // ── Step 1: 회원가입 batch ──
    for (let start = 1; start <= USER_COUNT; start += BATCH_SIZE) {
        const end      = Math.min(start + BATCH_SIZE - 1, USER_COUNT);
        const requests = [];

        for (let i = start; i <= end; i++) {
            requests.push({
                method: 'POST',
                url:    `${BASE_URL}/api/user/signup`,
                body:   JSON.stringify({
                    name:            `테스트유저${i}`,
                    email:           `testuser${i}@loadtest.com`,
                    password:        password,
                    confirmPassword: password,
                }),
                params: { headers },
            });
        }

        http.batch(requests);
        console.log(`회원가입 완료: ${end}/${USER_COUNT}`);
    }

    // ── Step 2: 로그인 batch → 토큰 수집 ──
    const tokens = [];

    for (let start = 1; start <= USER_COUNT; start += BATCH_SIZE) {
        const end      = Math.min(start + BATCH_SIZE - 1, USER_COUNT);
        const requests = [];

        for (let i = start; i <= end; i++) {
            requests.push({
                method: 'POST',
                url:    `${BASE_URL}/api/user/login`,
                body:   JSON.stringify({
                    email:    `testuser${i}@loadtest.com`,
                    password: password,
                }),
                params: { headers },
            });
        }

        const responses = http.batch(requests);

        for (const res of responses) {
            const body  = JSON.parse(res.body);
            const token = body?.data?.accessToken;
            if (token) tokens.push(token);
        }

        console.log(`로그인 완료: ${end}/${USER_COUNT}`);
    }

    console.log(`토큰 수집 완료: ${tokens.length}개`);
    return { tokens };
}

// ─────────────────────────────────────────
// default(): VU마다 실행
// 대기열 진입 → 폴링 → 주문 생성
// ─────────────────────────────────────────
export default function (data) {
    // VU마다 다른 토큰 사용
    const iterIndex = exec.scenario.iterationInTest;
    const token   = data.tokens[iterIndex % data.tokens.length];
    const headers = {
        'Content-Type':  'application/json',
        'Authorization': `Bearer ${token}`,
    };

    // ── Step 1: 대기열 진입 ──
    const enterRes = http.post(
        `${BASE_URL}/api/user/queue/enter`,
        JSON.stringify({ productId: PRODUCT_ID }),
        { headers }
    );

    // 품절(QUEUE_001)이면 즉시 종료
    if (enterRes.status === 400) {
        const errBody = JSON.parse(enterRes.body);
        if (errBody?.code === 'QUEUE_001') {
            console.log(`[ITER ${iterIndex}] 품절 → 대기열 진입 포기`);
            return;
        }
    }

    check(enterRes, { '대기열 진입 성공': (r) => r.status === 200 });

    let admissionToken = null;
    const enterBody    = JSON.parse(enterRes.body);

    if (enterBody?.data?.admitted) {
        // 바로 입장 가능
        admissionToken = enterBody.data.admissionToken;
    } else {
        // ── Step 2: 순번 올 때까지 폴링 ──
        for (let attempt = 0; attempt < 60; attempt++) {
            sleep(3);

            const statusRes  = http.get(
                `${BASE_URL}/api/user/queue/status?productId=${PRODUCT_ID}`,
                { headers }
            );
            const statusBody = JSON.parse(statusRes.body);

            if (statusBody?.data?.admitted) {
                admissionToken = statusBody.data.admissionToken;
                break;
            }

            // 폴링 중 품절 감지 시 조기 종료
            if (statusRes.status === 400 && statusBody?.code === 'QUEUE_001') {
                console.log(`[ITER ${iterIndex}] 폴링 중 품절 감지 → 종료`);
                return;
            }
        }
    }

    if (!admissionToken) {
        console.warn(`[ITER ${iterIndex}] 입장 토큰 획득 실패 (타임아웃)`);
        return;
    }

    // ── Step 3: 주문 생성 ──
    const checkoutToken = `checkout-${iterIndex}-${Date.now()}`;

    const orderRes = http.post(
        `${BASE_URL}/api/user/order/create`,
        JSON.stringify({
            checkoutToken:  checkoutToken,
            admissionToken: admissionToken,
            items: [{ productId: PRODUCT_ID, quantity: 1 }]
        }),
        { headers }
    );

    check(orderRes, { '주문 생성 성공': (r) => r.status === 200 });

    if (orderRes.status !== 200) {
        console.error(`[ITER ${iterIndex}] 주문 실패: ${orderRes.body}`);
    }
}