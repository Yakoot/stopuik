select  u.*, u.fio, i.uik from incomingsheet i join currentuikmembers u on (uik=id and i.fio=u.fio)
where U.uik_member_id not in (select uik_member from uik_history where year=2019) order by uik;


insert into uik_crime(id, crime_title) select crime_id, crime_title from incomingsheet;

insert into uik_member_history_crime(year, uik_member, crime_id)
select 2019, u.uik_member_id, crime_id from incomingsheet i join currentuikmembers u on (uik=id and i.fio=u.fio);
insert into uik_crime_links(uik_crime_id, link_title, link_url)
select crime_id, trim(regexp_replace(link_title1, '[\n\r]+', '', 'g')), link_url1 from incomingsheet where link_url1 is not null;
