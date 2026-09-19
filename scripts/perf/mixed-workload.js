import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8500';
const token = __ENV.TOKEN || '';
const albumId = __ENV.ALBUM_ID || '1';
const category1Id = __ENV.CATEGORY1_ID || '1';
const keyword = __ENV.KEYWORD || '儿童';
const thinkTime = Number.parseFloat(__ENV.THINK_TIME || '0');
const fixedRate = Number.parseInt(__ENV.FIXED_RATE || '0', 10);

const albumDetailDuration = new Trend('album_detail_duration', true);
const channelDuration = new Trend('channel_duration', true);
const searchDuration = new Trend('search_duration', true);
const subscribeDuration = new Trend('subscribe_duration', true);
const collectDuration = new Trend('collect_duration', true);
const businessErrorRate = new Rate('business_error_rate');
const albumDetailErrorRate = new Rate('album_detail_error_rate');
const channelErrorRate = new Rate('channel_error_rate');
const searchErrorRate = new Rate('search_error_rate');
const subscribeErrorRate = new Rate('subscribe_error_rate');
const collectErrorRate = new Rate('collect_error_rate');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: fixedRate > 0 ? {
    mixed_fixed_rate: {
      executor: 'constant-arrival-rate',
      rate: fixedRate,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 200,
      maxVUs: 300,
      gracefulStop: '10s',
    },
  } : {
    mixed_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 200 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '20s' }],
    business_error_rate: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '20s' }],
    checks: ['rate>0.99'],
    ...(fixedRate > 0 ? { dropped_iterations: ['count==0'] } : {}),
  },
};

const authParams = token ? { headers: { token } } : {};
const jsonParams = {
  headers: {
    'Content-Type': 'application/json',
    ...(token ? { token } : {}),
  },
};

function verify(response, name, endpointErrorRate) {
  let businessSuccess = false;
  try {
    businessSuccess = JSON.parse(response.body).code === 200;
  } catch (_) {
    businessSuccess = false;
  }
  const success = response.status === 200 && businessSuccess;
  businessErrorRate.add(!success);
  endpointErrorRate.add(!success);
  check(response, {
    [`${name}: http 200`]: (res) => res.status === 200,
    [`${name}: business success`]: () => businessSuccess,
  });
}

export default function () {
  const draw = Math.random();
  let response;
  if (draw < 0.45) {
    response = http.get(`${baseUrl}/api/search/albumInfo/${albumId}`);
    albumDetailDuration.add(response.timings.duration);
    verify(response, 'album-detail', albumDetailErrorRate);
  } else if (draw < 0.65) {
    response = http.get(`${baseUrl}/api/search/albumInfo/channel/${category1Id}`);
    channelDuration.add(response.timings.duration);
    verify(response, 'channel', channelErrorRate);
  } else if (draw < 0.85) {
    response = http.post(`${baseUrl}/api/search/albumInfo`, JSON.stringify({
      keyword,
      category1Id: 0,
      category2Id: 0,
      category3Id: 0,
      attributeList: [],
      order: '1:desc',
      pageNo: 1,
      pageSize: 10,
    }), jsonParams);
    searchDuration.add(response.timings.duration);
    verify(response, 'search', searchErrorRate);
  } else if (token && draw < 0.93) {
    response = http.get(`${baseUrl}/api/user/userInfo/findUserSubscribePage/1/20`, authParams);
    subscribeDuration.add(response.timings.duration);
    verify(response, 'subscribe-page', subscribeErrorRate);
  } else if (token) {
    response = http.get(`${baseUrl}/api/user/userInfo/findUserCollectPage/1/20`, authParams);
    collectDuration.add(response.timings.duration);
    verify(response, 'collect-page', collectErrorRate);
  } else {
    response = http.get(`${baseUrl}/api/search/albumInfo/${albumId}`);
    albumDetailDuration.add(response.timings.duration);
    verify(response, 'album-detail-no-token', albumDetailErrorRate);
  }
  if (thinkTime > 0) {
    sleep(thinkTime);
  }
}

export function handleSummary(data) {
  const output = {};
  output[__ENV.SUMMARY_PATH || 'results/summary.json'] = JSON.stringify(data, null, 2);
  return output;
}
