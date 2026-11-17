--liquibase formatted sql

-- changeset bsports-club:v16-accesos-club-member-estado
-- comment: Snapshot del socio y estado al registrar acceso al club
ALTER TABLE accesos_club
  ADD COLUMN member_id BIGINT NULL AFTER user_id,
  ADD COLUMN member_estado VARCHAR(20) NULL AFTER tipo,
  ADD KEY fk_accesos_club_member (member_id),
  ADD CONSTRAINT fk_accesos_club_member FOREIGN KEY (member_id) REFERENCES members (id);
