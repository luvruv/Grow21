const express = require('express');
const router = express.Router();
const apiController = require('../controllers/apiController');

// Auth
router.post('/login', apiController.login);
router.post('/signup', apiController.signup);
router.post('/validate', apiController.validateToken);
router.post('/forgot-password', apiController.forgotPassword);
router.post('/reset-password', apiController.resetPassword);
router.post('/child-login', apiController.childLogin);

// 1. Dashboard
router.get('/dashboard', apiController.getDashboardStats);

// 2. Parents
router.get('/parents', apiController.getAllParents);
router.post('/parents', apiController.addParent);

// 3. Children
router.get('/children', apiController.getAllChildren);
router.get('/children/details', apiController.getChildWithParent); // The JOIN query
router.post('/children', apiController.addChild);

// 4. Sessions
router.get('/sessions/:childId', apiController.getSessionsByChild);
router.post('/sessions', apiController.addSession);

// 5. Progress
router.get('/progress/:childId', apiController.getProgressByChild);
router.get('/progress/report/:childId', apiController.callProgressStoredProcedure);

// 6. App Sync
router.post('/app-sync', apiController.appSession);

module.exports = router;
