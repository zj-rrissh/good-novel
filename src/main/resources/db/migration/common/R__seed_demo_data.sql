INSERT INTO user_account (id, username, password_hash, status, roles, login_version)
SELECT 1001, 'reader_demo', '$2b$10$jGQav3HMeq102vlU4U5ZtO37Wv0EljahpIsMYuiyzgvtBQ9gZozxq', 'NORMAL', 'USER', 1
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM user_account WHERE id = 1001);

INSERT INTO user_account (id, username, password_hash, status, roles, login_version)
SELECT 1002, 'author_demo', '$2b$10$NeJg7hhnZReRR5BWF5TmB.1fFvAZi.Z8duIHmf/yly4GkZrHQDjl.', 'NORMAL', 'USER,AUTHOR', 1
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM user_account WHERE id = 1002);

INSERT INTO user_account (id, username, password_hash, status, roles, login_version)
SELECT 1003, 'admin_demo', '$2b$10$/xkQ55tl1NDmBHJdwsiLMuCSGrok0SwLU/LWOje560U9zydjbwhjW', 'NORMAL', 'USER,AUTHOR,ADMIN,REVIEWER', 1
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM user_account WHERE id = 1003);

INSERT INTO user_profile (user_id, nickname, avatar_url, bio, level, verified_status)
SELECT 1001, 'Reader Demo', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=256&q=80', 'Demo reader account for local testing.', 3, 'UNVERIFIED'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM user_profile WHERE user_id = 1001);

INSERT INTO user_profile (user_id, nickname, avatar_url, bio, level, verified_status)
SELECT 1002, 'Author Demo', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=256&q=80', 'Demo author account with seeded novels.', 5, 'VERIFIED'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM user_profile WHERE user_id = 1002);

INSERT INTO user_profile (user_id, nickname, avatar_url, bio, level, verified_status)
SELECT 1003, 'Admin Demo', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=256&q=80', 'Demo admin and reviewer account.', 7, 'VERIFIED'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM user_profile WHERE user_id = 1003);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
SELECT 1, 1002, 'sample novel', 'A starter fantasy story for homepage and detail-page smoke testing.', 'https://example.com/cover.png', 1001, '1,2,3', 'ON_SHELF', 11001, 2180, NULL
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel WHERE id = 1);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
SELECT 2, 1002, 'Clockwork Frontier', 'When steam engines learn to dream, the border city stops sleeping.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=720&q=80', 1002, '2,5,8', 'ON_SHELF', 11002, 2640, NULL
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel WHERE id = 2);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
SELECT 3, 1002, 'Ocean of Ash', 'An exiled navigator returns to map a sea that burns at night.', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=720&q=80', 1003, '3,6,9', 'ON_SHELF', 11003, 2310, NULL
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel WHERE id = 3);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
SELECT 4, 1002, 'Last Light Academy', 'A failing student discovers the school library can rewrite fate.', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=720&q=80', 1004, '4,7,10', 'ON_SHELF', 11004, 2890, NULL
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel WHERE id = 4);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
SELECT 5, 1002, 'Night Market Chronicles', 'Each midnight bargain grants power and takes a memory.', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=720&q=80', 1005, '5,11,12', 'ON_SHELF', 11005, 2475, NULL
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel WHERE id = 5);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
SELECT 6, 1002, 'Starrail Apothecary', 'A healer rides orbital trains to cure worlds under quarantine.', 'https://images.unsplash.com/photo-1513128034602-7814ccaddd4e?auto=format&fit=crop&w=720&q=80', 1006, '6,13,14', 'ON_SHELF', 11006, 2760, NULL
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel WHERE id = 6);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
SELECT 11001, 1, 1, 'Chapter 1: Arrival', 'Rain hammered the station glass when Lin stepped into Ironbridge. The city map said this was a frontier town, but every street had five stories of history and one rumor about monsters in the river fog.', 'PUBLISHED', NULL, '2026-01-10 10:00:00.000'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel_chapter WHERE id = 11001);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
SELECT 11002, 2, 1, 'Chapter 1: Brass Dawn', 'At dawn the boiler towers exhaled white clouds over Clockwork Frontier. Mara counted the pulses in the steam lines, because each pulse matched a name erased from the census.', 'PUBLISHED', NULL, '2026-01-10 10:10:00.000'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel_chapter WHERE id = 11002);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
SELECT 11003, 3, 1, 'Chapter 1: Burning Tide', 'The harbor master warned that no ship survived the Ash Ocean after sunset. Rook paid double for a lantern anyway and asked where the old star charts were buried.', 'PUBLISHED', NULL, '2026-01-10 10:20:00.000'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel_chapter WHERE id = 11003);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
SELECT 11004, 4, 1, 'Chapter 1: Borrowed Page', 'The detention room had one cracked window and one forbidden book. When Mei opened page thirteen, tomorrow changed its mind about her exam results.', 'PUBLISHED', NULL, '2026-01-10 10:30:00.000'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel_chapter WHERE id = 11004);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
SELECT 11005, 5, 1, 'Chapter 1: Midnight Price', 'At exactly 00:00, the lanterns in Night Market turned blue. Every merchant offered one miracle and demanded one memory in return.', 'PUBLISHED', NULL, '2026-01-10 10:40:00.000'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel_chapter WHERE id = 11005);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
SELECT 11006, 6, 1, 'Chapter 1: Quarantine Orbit', 'The starrail stopped above a silent colony where no signals had left for three weeks. Toma packed medicine, then hid his fear in a joke nobody laughed at.', 'PUBLISHED', NULL, '2026-01-10 10:50:00.000'
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM novel_chapter WHERE id = 11006);

INSERT INTO reading_progress (user_id, novel_id, chapter_id, progress_percent, page_offset)
SELECT 1001, 1, 11001, 62, 2048
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM reading_progress WHERE user_id = 1001 AND novel_id = 1);

INSERT INTO reading_progress (user_id, novel_id, chapter_id, progress_percent, page_offset)
SELECT 1001, 2, 11002, 18, 512
WHERE ${seed_demo_enabled} = 1
  AND NOT EXISTS (SELECT 1 FROM reading_progress WHERE user_id = 1001 AND novel_id = 2);
