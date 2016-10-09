var express = require('express');
var bodyParser = require('body-parser');
var app = express();

var players = {
	'2': {
		x:1,
		y:1,
		team: 'red'
	},
	'4': {
		x:8,
		y:1,
		team: 'red'
	},
	'6': {
		x:8,
		y:5,
		team: 'red'
	},
	'8': {
		x:1,
		y:5,
		team: 'red'
	}
};

app.use(bodyParser.json());

app.get('/aps', function(req, res) {
	console.log("bindeado");
	var aps = [
    {
      bssid: "00:25:9c:8e:da:2c",
      x: 0,
      y: 0
    },
    {
      bssid: "1c:bd:b9:b3:33:8e",
      x: 9,
      y: 0
    },
    {
      bssid: "d8:61:94:3a:b3:11",
      x: 1,
      y: 6
    }
  ];
  return res.json(aps);
});

app.post('/player/:id', function(req, res) {
	console.log(req.body)
	players[req.params.id] = {x: req.body.x, y: req.body.y, team: (req.params.id % 2 == 0)? 'red' :'blue'};
	return res.send();
});

app.get('/map', function(req, res) {
  return res.json(players);
});

app.listen(3000, function () {
  console.log('Example app listening on port 3000!');
});