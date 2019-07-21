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

lrmRoutesFoundEvent = {
    routes: {}
}

lrmRoute = {
    coordinates: {},
    summary: {
        totalDistance: {}
    }
}

mapClickEvent = {
    latlng: {
        lat: {},
        lng: {}
    }
}