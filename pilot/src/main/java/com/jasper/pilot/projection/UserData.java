package com.jasper.pilot.projection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "UserData")
public class UserData {

	@Id
	private String id;
	private String month;
	private int users;
	private int sessions;
	private int pageViews;
}
