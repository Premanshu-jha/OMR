package org.example.studentdashboard.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.studentdashboard.Models.Student;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTService {

    @Value("${SECRET_KEY}")
    private String SECRET;

    public String createToken(Student student){
        Map<String,Object> claims = new HashMap<>();
        claims.put("id",student.getId());
        claims.put("name",student.getName());
        claims.put("phone",student.getPhone());
        claims.put("city",student.getCity());
        claims.put("classNum",student.getClassNum());
        claims.put("role",student.getRole());

        return Jwts.builder()
                .addClaims(claims)
                .setSubject(student.getRollNo())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000*60*60*24))
                .signWith(generateKey())
                .compact();

    }

    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(generateKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String getRollNumber(String token){
         return extractAllClaims(token).getSubject();
    }

    private boolean isTokenExpired(String token){
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token,Student student){
         return (student.getRollNo().equals(getRollNumber(token))) && !isTokenExpired(token);
    }

    private Key generateKey(){
        byte[] keysBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keysBytes);
    }

}
