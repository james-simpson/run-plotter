-- :name sql-insert-route
-- :doc Insert a route
-- :command :returning-execute
insert into routes (name, distance, polyline)
values (:name, :distance, :polyline)
returning id;

-- :name sql-select-route
-- :doc Select a routes with waypoints
select * from routes
where id = :id;

-- :name sql-select-all-routes
-- :doc Select all routes
select * from routes

-- :name sql-delete-route!
-- :doc Delete a route
-- :command :execute
delete from routes
where id = :id;