// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
angular.module('starter', ['ionic'])

.run(function($ionicPlatform) {
  $ionicPlatform.ready(function() {
    if(window.cordova && window.cordova.plugins.Keyboard) {
      // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard
      // for form inputs)
      cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);

      // Don't remove this line unless you know what you are doing. It stops the viewport
      // from snapping when text inputs are focused. Ionic handles this internally for
      // a much nicer keyboard experience.
      cordova.plugins.Keyboard.disableScroll(true);
    }
    if(window.StatusBar) {
      StatusBar.styleDefault();
    }
  });
})
.controller('MainCtrl', function($ionicPlatform, $http, $interval, $timeout){
  var _this = this;
  var configured = false;
  var canvas = document.getElementById('map');
  var context = canvas.getContext('2d');
//  var team = (device.uuid % 2 == 0)?"red":"blue";
  this.team = 'blue';
  this.UAV = false;
  var realoadingUAV = false;

  this.sendUAV = function(){
    _this.UAV = true;
    $timeout(function(){
        _this.UAV = false;
        _this.reloadingUAV = true;
    },5000);
    $timeout(function(){
        _this.reloadingUAV = false;
    },15000);
  }

  this.setup = function(){
    $http({
      method: 'GET',
      url: 'http://10.0.0.123:3000/aps'
    }).then(function successCallback(response) {
        _this.setupToNative(response.data);
      }, function errorCallback(response) {
        _this.setup();
    });
  };

  this.setupToNative = function(data){
    window.trilateration.setup(function(success){
      _this.configured = true;
      //_this.getPosition()
    },function(error){
      _this.setup();
      _this.casa = error;
    },data);

  };

  this.getPosition = function(){
    window.trilateration.getPosition(function(success){
      $http({
         method: 'POST',
         data: success,
         url: 'http://10.0.0.123:3000/player/'+device.uuid
      }).then(function successCallback(response) {
      }, function errorCallback(response) {
      });

    },function(error){
    });
  };

  this.getMap = function(){
    $http({
      method: 'GET',
      url: 'http://10.0.0.123:3000/map'
    }).then(function successCallback(response) {
        context.clearRect(0, 0, canvas.width, canvas.height);
        for(i in response.data){
            var player = response.data[i];
            drawPlayer(i,player.x, player.y, player.team);
        }
      }, function errorCallback(response) {

    });
  };

  function drawPlayer(id, x,y,team) {
    //530x360
    //6x9
    // 88 x 40
    if (team != _this.team && !_this.UAV){
        return false;
    }
    var xx = x * (canvas.width/9);
    var yy = y * (canvas.height/6);
    context.beginPath();
    context.arc(xx, yy, 5, 0, 2*Math.PI, false);
    context.fillStyle = team;
    if(id == device.uuid){
        context.fillStyle = 'green';
    }
    //context.fillStyle = (friend)?"#0f0":"#f00";
    context.fill();
    context.lineWidth = 1;
    context.strokeStyle = "#666666";
    context.stroke();  
  }

  $interval(function() {
    _this.getPosition();
  },1000);

  $interval(function() {
    _this.getMap();
  },2000);
})
