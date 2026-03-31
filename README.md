*GIT COMMANDS*

// kiểm tra các remote:
git remote -v

// thêm remote:
git remote add <TÊN_REMOTE> <URL_REPO>

// xóa remote:
git remote remove <TÊN_REMOTE>

// lệnh pull:
git pull <tên_remote> <tên_nhánh>
ví dụ: git pull backup main

// lệnh push:
git push <tên_remote> <tên_nhánh>
ví dụ: git push origin main
       
// lệnh loại bỏ mọi thứ và quay về head của remote bạn muốn: 
git reset --hard <tên_remote>/<tên_nhánh>
