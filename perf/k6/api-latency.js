// Load/latency smoke test of the backend API.
//   k6 run -e BASE_URL=http://localhost:8080 perf/k6/api-latency.js
//   k6 run --summary-export=target/k6-summary.json perf/k6/api-latency.js     (machine-readable result)
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  vus: 10,            // 10 virtual users at the same time
  duration: '30s',
  thresholds: {       // the run FAILS (exit code 99) when any of these is not met
    http_req_failed: ['rate<0.01'],                          // < 1 % errors
    http_req_duration: ['p(95)<500'],                        // 95 % of all requests under 500 ms
    'http_req_duration{endpoint:list_books}': ['p(95)<500'],
    'http_req_duration{endpoint:start_game}': ['p(95)<500'],
    'http_req_duration{endpoint:get_game}': ['p(95)<500'],
  },
};

// runs once before the load: picks an existing book instead of assuming that id 1 exists
export function setup() {
  const books = http.get(`${BASE}/api/books`).json();
  return { bookId: books[0].id };
}

export default function (data) {
  const list = http.get(`${BASE}/api/books`, { tags: { endpoint: 'list_books' } });
  check(list, { 'list books: 200': (r) => r.status === 200 });

  const start = http.post(`${BASE}/api/games`, JSON.stringify({ bookId: data.bookId }),
    { headers: JSON_HEADERS, tags: { endpoint: 'start_game' } });
  check(start, { 'start game: 201': (r) => r.status === 201 });

  const state = http.get(`${BASE}/api/games/${start.json('sessionId')}`, { tags: { endpoint: 'get_game' } });
  check(state, { 'get game: 200': (r) => r.status === 200 });

  sleep(0.2);
}
