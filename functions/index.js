/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
//
// region: Firestore DB 위치(asia-northeast3, 서울)와 맞춘다.
// Firestore 트리거는 DB와 같은 지역이어야 하고, 같은 지역이면 지연·비용도 줄어든다.
// 바꾸면 앱 FirebaseModule.provideFunction의 getInstance("...") 지역도 같이 바꿀 것
// (다르면 앱이 함수를 찾지 못해 NOT_FOUND).
// minInstances는 지정하지 않는다(기본 0) → 요청이 없을 때 과금 없음, 콜드 스타트는 감수.
setGlobalOptions({
  region: "asia-northeast3",
  maxInstances: 10,
});

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
