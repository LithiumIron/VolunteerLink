begin;

-- Organisation Create uploads into this bucket. Volunteers only need
-- authenticated SELECT access to render the published opportunity thumbnail.
drop policy if exists authenticated_read_post_thumbnails
on storage.objects;

create policy authenticated_read_post_thumbnails
on storage.objects
for select
to authenticated
using (bucket_id = 'post-thumbnails');

commit;
