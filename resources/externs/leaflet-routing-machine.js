// externs for leaflet icon glyph
L.icon = {
    glyph: function() {}
}

// externs for leaflet routing machine
L.Routing = {
    control: {
        on: function() {},
        addTo: function() {},
        setWaypoints: function() {}
    },
    mapbox: function() {}
}

var lrmRoutesFoundEvent = {
    routes: {}
}

var lrmRoute = {
    coordinates: {},
    summary: {
        totalDistance: {}
    }
}

var mapClickEvent = {
    latlng: {
        lat: {},
        lng: {}
    }
}