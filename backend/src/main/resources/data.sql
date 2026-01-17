-- 테스트 사용자 생성 (없으면 삽입)
INSERT INTO users (id, username, display_name, status)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'testuser', '테스트 사용자', 'ONLINE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, display_name, status)
VALUES ('550e8400-e29b-41d4-a716-446655440099', 'testuser2', '테스트 사용자2', 'ONLINE')
ON CONFLICT (id) DO NOTHING;

-- 테스트 채팅방 생성 (없으면 삽입)
INSERT INTO chat_rooms (id, name, description, type, created_by)
VALUES ('550e8400-e29b-41d4-a716-446655440001', '일반 채팅방', '자유롭게 대화하세요', 'GROUP', '550e8400-e29b-41d4-a716-446655440000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_rooms (id, name, description, type, created_by)
VALUES ('550e8400-e29b-41d4-a716-446655440002', '개발팀', '개발 관련 대화', 'GROUP', '550e8400-e29b-41d4-a716-446655440000')
ON CONFLICT (id) DO NOTHING;

-- 테스트 사용자를 채팅방에 추가
INSERT INTO room_members (room_id, user_id, role)
VALUES ('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'OWNER')
ON CONFLICT (room_id, user_id) DO NOTHING;

INSERT INTO room_members (room_id, user_id, role)
VALUES ('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', 'OWNER')
ON CONFLICT (room_id, user_id) DO NOTHING;
