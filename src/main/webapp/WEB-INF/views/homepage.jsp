<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title>SEARCH KEYWORD</title>
</head>
<body>
    <h1>검색할 KEYWORD를 입력해주세요.</h1>
    <form action='/search/news' method='POST'>
        키워드 : <input type='text' name="name">
        <input type='submit' value="submit">
    </form>
</body>
</html>