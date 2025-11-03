package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface UserMappingHelper {
	
	public static UserDto map(final User user) {
		return UserDto.builder()
				.userId(user.getUserId())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.imageUrl(user.getImageUrl())
				.email(user.getEmail())
				.phone(user.getPhone())
				.credentialDto(
						CredentialDto.builder()
							.credentialId(user.getCredential().getCredentialId())
							.username(user.getCredential().getUsername())
							.password(user.getCredential().getPassword())
							.roleBasedAuthority(user.getCredential().getRoleBasedAuthority())
							.isEnabled(user.getCredential().getIsEnabled())
							.isAccountNonExpired(user.getCredential().getIsAccountNonExpired())
							.isAccountNonLocked(user.getCredential().getIsAccountNonLocked())
							.isCredentialsNonExpired(user.getCredential().getIsCredentialsNonExpired())
							.build())
				.build();
	}
	
	public static User map(final UserDto userDto) {
		// Solo establecer userId si NO es null (para updates)
		// Para nuevas entidades, dejar que JPA genere el ID
		User.UserBuilder userBuilder = User.builder()
				.firstName(userDto.getFirstName())
				.lastName(userDto.getLastName())
				.imageUrl(userDto.getImageUrl())
				.email(userDto.getEmail())
				.phone(userDto.getPhone());
		
		// Solo establecer el userId si existe (update), no en creación (create)
		if (userDto.getUserId() != null) {
			userBuilder.userId(userDto.getUserId());
		}
		
		User user = userBuilder.build();
		
		// Solo crear credential si el DTO lo incluye
		if (userDto.getCredentialDto() != null) {
			Credential.CredentialBuilder credentialBuilder = Credential.builder()
					.username(userDto.getCredentialDto().getUsername())
					.password(userDto.getCredentialDto().getPassword())
					.roleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority())
					.isEnabled(userDto.getCredentialDto().getIsEnabled())
					.isAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired())
					.isAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked())
					.isCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired())
					.user(user);  // Establecer la relación bidireccional
			
			// Solo establecer credentialId si existe (update)
			if (userDto.getCredentialDto().getCredentialId() != null) {
				credentialBuilder.credentialId(userDto.getCredentialDto().getCredentialId());
			}
			
			Credential credential = credentialBuilder.build();
			user.setCredential(credential);
		}
		
		return user;
	}
	
	
	
}






