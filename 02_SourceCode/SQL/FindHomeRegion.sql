update
	aa_Happenings
set
	parameterRegion =(
	select
		cat
	from
		aa_parameterregions
	where
		ST_Intersects(aa_Happenings.adjustedGeom,
		aa_parameterregions.GEOMETRY) )
where
	aa_happenings.geometry is not null;

update
	aa_mappers
set
	homeRegion =(
	select
		regionid
	from
		(
		select
			mapperId as mid2,
			regionId,
			MAX(nrEdits)
		from
			aa_MapperRegionRelation
		JOIN aa_parameterregions ON
			regionId = cat
		join (
			select
				mapperid as mid1,
				min(happeningid),
				cult as cultFix
			from
				aa_happeningMapperJoin
			join aa_parameterregions on
				cat = parameterregion
			where
				parameterregion is not null
			group by
				MapperId ) on
			mid1 = mapperid
		where
			cult = cultFix
		group by
			mapperid)
	where
		mid2 = aa_mappers.id)
where
	homeRegion is NULL;

UPDATE
	aa_Mappers
set
	homeregion =(
	select
		pam
	from
		(
		select
			parameterRegion as pam,
			min(happeningid),
			mapperid as mid
		from
			aa_happeningMapperJoin
		group by
			mapperid)
	where
		aa_mappers.id = mid)
where
	homeRegion is null;

update
	aa_mappers
set
	homeregion = (
	select
		rid
	from
		(
		select
			mapperid as mid,
			max(nredits),
			regionid as rid
		from
			aa_MapperRegionRelation
		group by
			mapperid)
	where
		mid = aa_mappers.id)
where
	homeregion is null;
