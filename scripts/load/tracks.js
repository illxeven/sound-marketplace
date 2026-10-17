import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    browse_tracks: {
      executor: 'constant-vus',
      vus: 50,
      duration: '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // < 1% errors
    http_req_duration: ['p(95)<200'], // 95% of requests must complete below 200ms
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export default function () {
  // Simulate user browsing first 5 pages with default page size 20
  for (let page = 0; page < 5; page++) {
    const res = http.get(`${BASE_URL}/api/tracks?page=${page}&size=20&sort=createdAt,desc`, {
      headers: {
        'Accept': 'application/json',
      },
      tags: { name: 'GetTracksPage' },
    });

    check(res, {
      'status is 200': (r) => r.status === 200,
      'has success true': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.success === true;
        } catch (_) {
          return false;
        }
      },
      'has items array': (r) => {
        try {
          const body = JSON.parse(r.body);
          return Array.isArray(body.data.items);
        } catch (_) {
          return false;
        }
      },
      'payload size under 50KB': (r) => r.body.length < 51200,
      'response time p95 < 200ms': (r) => r.timings.duration < 200,
    });

    // Realistic think time between page switches (200ms - 600ms)
    sleep(0.2 + Math.random() * 0.4);
  }
}
