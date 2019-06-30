-- :name sql-insert-route
-- :doc Insert a route
-- :command :returning-execute
insert into routes (name, distance)
values (:name, :distance)
returning id;

-- :name sql-insert-waypoints
-- :doc Insert a route
-- :command :execute
insert into waypoints (route_id, waypoint_order, lat, lng)
values :tuple*:waypoints;

-- :name sql-select-route
-- :doc Select a routes with waypoints
select * from routes
join waypoints on routes.id = waypoints.route_id
where id = :id;

-- :name sql-select-all-routes
-- :doc Select all routes
select * from routes

-- :name sql-delete-route!
-- :doc Delete a route
-- :command :execute
delete from routes
where id = :id;