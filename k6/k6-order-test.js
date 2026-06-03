import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        limited_sale: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: 100000,
            maxDuration: '10m',
        },
    },
};

export default function () {

    const payload = JSON.stringify({
        productId: 45,
        quantity: 1
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJzdW5namluQGdtYWlsLmNvbSIsInVzZXJJZCI6NCwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzgwNDc2ODQwLCJleHAiOjE3ODA1NjMyNDB9.KSWsQxu-zmGXRPSdRLN0739Re3g-0ThWlQnSYNMVSKo-SfUaKNY_wgTly6qR-PNB'
        }
    };

    const res = http.post(
        'http://localhost:8080/api/user/order/create',
        payload,
        params
    );

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        `status=${res.status}, body=${res.body}`
    }
    // console.log(
    //     `status=${res.status}, body=${res.body}`
    // );
}