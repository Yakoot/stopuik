select  u.*, u.fio, i.uik from incomingsheet i join currentuikmembers u on (uik=id and i.fio=u.fio)
where U.uik_member_id not in (select uik_member from uik_history where year=2019) order by uik;


insert into uik_crime(id, crime_title) select crime_id, crime_title from incomingsheet;

insert into uik_member_history_crime(year, uik_member, crime_id)
select 2019, u.uik_member_id, crime_id from incomingsheet i join currentuikmembers u on (uik=id and i.fio=u.fio);
insert into uik_crime_links(uik_crime_id, link_title, link_url)
select crime_id, trim(regexp_replace(link_title1, '[\n\r]+', '', 'g')), link_url1 from incomingsheet where link_url1 is not null;


insert into crimetype(value, comment) values
('нерассмотрение жалоб или отказ в их принятии', 'только для председателей, заместителей или секретарей'),
('ненадлежащее оповещение о заседаниях', 'только для председателей, заместителей или секретарей'),
('участие в незаконном бездействии', 'в случае, если не голосовали за принятие решения, что в последующем признано как незаконное бездействие, а также если голосовали за принятие решения, которое затем было обжаловано и признано незаконное бездействие'),
('участие в принятии незаконного решения', ''),
('превышение полномочий члена, должностного лица ТИК', ''),
('введение в заблуждение', ''),
('создание препятствий осуществлению прав участников избирательного процесса', ''),
('нарушения при учете избирательных бюллетеней, документов строгой отчетности', ''),
('подписание документов, заведомо противоречащих требованиям закона', 'напр. подписание решения комиссии, принятого на заседании при отсутствии кворума, либо в случае если за него проголосовало недостаточное количество членов комиссии'),
('сокрытие информации', ''),
('участие в фальсификации итогов голосования или результатов выборов', ''),
('укрывательство незаконных действий членов нижестоящих комиссий', '');
