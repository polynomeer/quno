-- Phase 71 (ADR-0073): an answer can record the live chat room it was summarised from, so the
-- question card can show "질문방 대화에서 정리됨". Rooms never close, so the link stays valid.
ALTER TABLE answers
    ADD COLUMN source_live_chat_room_id BIGINT NULL REFERENCES live_chat_rooms(id);
