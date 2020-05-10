const xyzPoints = JSON.parse("[[51.43707,-2.59093,19.30000000000109],[51.43641,-2.59087,17],[51.43635,-2.59274,16.5],[51.43635,-2.59275,16.5],[51.43632,-2.59301,16.400000000001455],[51.43581,-2.59494,16.100000000000364],[51.43558,-2.59667,16.600000000000364],[51.43556,-2.59704,13.800000000001091],[51.4356,-2.59789,13.100000000000364],[51.43563,-2.59842,14],[51.43542,-2.59843,13.100000000000364],[51.43521,-2.59851,12.600000000000364],[51.43487,-2.59845,12.400000000001455],[51.43436,-2.59854,14.300000000001091],[51.43371,-2.59884,11.900000000001455],[51.43326,-2.59878,12.900000000001455],[51.43302,-2.59874,12.200000000000728],[51.43279,-2.59879,12],[51.43282,-2.59954,12.100000000000364],[51.43234,-2.59962,12],[51.43174,-2.59959,11.100000000000364],[51.43115,-2.59991,11.400000000001455],[51.43093,-2.60014,11.200000000000728],[51.43073,-2.60025,11.400000000001455],[51.43072,-2.60026,11.400000000001455],[51.43033,-2.60055,12.600000000000364],[51.43101,-2.60238,12.700000000000728],[51.43022,-2.6031,13.100000000000364],[51.43014,-2.60336,13.700000000000728],[51.43014,-2.60337,13.700000000000728],[51.43001,-2.60461,17.700000000000728],[51.42895,-2.6043,15.200000000000728],[51.42882,-2.60428,15.300000000001091],[51.42882,-2.60438,15.300000000001091],[51.42847,-2.60431,15.900000000001455],[51.42843,-2.60452,16],[51.42828,-2.60471,16.200000000000728],[51.4271,-2.60511,17.200000000000728],[51.42559,-2.60506,17.600000000000364],[51.42554,-2.60514,18.200000000000728],[51.42547,-2.6054,19.30000000000109],[51.42552,-2.60583,19.80000000000109],[51.42546,-2.60609,19.600000000000364],[51.42477,-2.6071,18.400000000001455],[51.42477,-2.60711,18.400000000001455],[51.4243,-2.60786,19.200000000000728],[51.42357,-2.60866,20.200000000000728],[51.42341,-2.60971,24.600000000000364],[51.42324,-2.61015,27.80000000000109],[51.42281,-2.611,32.70000000000073],[51.42235,-2.61241,35.100000000000364],[51.42191,-2.61284,30.700000000000728],[51.42074,-2.6153,36.400000000001455],[51.41904,-2.61638,39.5],[51.41872,-2.61678,38.30000000000109],[51.41863,-2.61647,36.900000000001455],[51.41853,-2.6161,37.400000000001455],[51.41858,-2.61586,38.400000000001455],[51.41912,-2.6153,40.70000000000073],[51.41976,-2.61419,45.5],[51.41976,-2.6142,45.5],[51.42024,-2.61347,48.400000000001455],[51.42042,-2.61301,49.5],[51.42063,-2.61229,49.80000000000109],[51.42087,-2.61103,48.80000000000109],[51.42154,-2.60883,49.400000000001455],[51.42177,-2.60848,49],[51.42218,-2.60818,45.80000000000109],[51.42332,-2.60598,33],[51.42451,-2.60548,22.30000000000109],[51.42535,-2.60514,19.400000000001455],[51.42549,-2.60492,17.80000000000109],[51.42701,-2.60513,17.400000000001455],[51.42975,-2.60453,17],[51.42974,-2.60453,17],[51.43019,-2.60314,13.200000000000728],[51.43079,-2.60022,11.200000000000728],[51.43078,-2.60022,11.300000000001091],[51.43563,-2.59835,13.900000000001455],[51.4356,-2.59777,12.800000000001091],[51.43572,-2.59558,17.30000000000109],[51.43572,-2.59559,17.30000000000109],[51.43589,-2.59455,16],[51.43706,-2.59093,19.30000000000109]]")

const y = xyzPoints.map(p => p[2])

Plotly.newPlot('myDiv', [{
  type: 'scatter',
  mode: 'lines',
  y: y,
  opacity: 1,
  line: {
    width: 6,
    reversescale: false
  }
}], {
  height: 640
});
