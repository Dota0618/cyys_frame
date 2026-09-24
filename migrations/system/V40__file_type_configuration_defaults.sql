-- 文件类型全局参数；三个参数都为空时保持不限制文件类型。
INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-file-files', 'FILE_FILES', '', 1,
       '普通文件允许的扩展名，使用逗号分隔，例如 pdf,doc,docx；与 FILE_MEDIA、FILE_IMAGES 合并为上传白名单。', 130
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'FILE_FILES');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-file-media', 'FILE_MEDIA', '', 1,
       '音频和视频文件允许的扩展名，使用逗号分隔，例如 mp3,mp4；与 FILE_FILES、FILE_IMAGES 合并为上传白名单。', 140
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'FILE_MEDIA');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-file-images', 'FILE_IMAGES', '', 1,
       '图片文件允许的扩展名，使用逗号分隔，例如 jpg,jpeg,png；与 FILE_FILES、FILE_MEDIA 合并为上传白名单。', 150
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'FILE_IMAGES');
