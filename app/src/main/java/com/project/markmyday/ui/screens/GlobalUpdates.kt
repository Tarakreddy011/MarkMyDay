package com.project.markmyday.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.project.markmyday.R
import com.project.markmyday.data.model.GNewsArticle
import com.project.markmyday.ui.components.DashboardBottomBar
import com.project.markmyday.ui.components.DashboardTopBar
import com.project.markmyday.viewmodel.GlobalUpdatesViewModel
import com.project.markmyday.viewmodel.NewsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalUpdateScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: GlobalUpdatesViewModel = viewModel()
) {
    val hotNewsState by viewModel.hotNews.collectAsState()
    val nationalNewsState by viewModel.nationalNews.collectAsState()
    val internationalNewsState by viewModel.internationalNews.collectAsState()

    Scaffold(
        topBar = {
            DashboardTopBar(
                title = "Global Updates",
                onBackClick = onBack,
                onNotificationClick = { /* Handle notifications */ },
                isBoldBackIcon = true
            )
        },
        bottomBar = {
            DashboardBottomBar(
                currentRoute = "global_updates",
                onNavigate = onNavigate
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Hot Headlines Carousel
            item {
                HorizontalNewsSection(
                    title = "🔥 Top Headlines",
                    state = hotNewsState
                )
            }

            // 2. National News (India)
            item {
                VerticalNewsSection(
                    title = "🇮🇳 Top Stories in India",
                    state = nationalNewsState
                )
            }

            // 3. International News
            item {
                VerticalNewsSection(
                    title = "🌎 World News",
                    state = internationalNewsState
                )
            }
        }
    }
}

@Composable
fun HorizontalNewsSection(title: String, state: NewsState) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        when (state) {
            is NewsState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            is NewsState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.articles) { article ->
                        HotNewsCard(article)
                    }
                }
            }
            is NewsState.Error -> {
                Text(text = state.message, modifier = Modifier.padding(20.dp), color = Color.Gray)
            }
        }
    }
}

@Composable
fun HotNewsCard(article: GNewsArticle) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(200.dp)
            .clickable {
                article.url?.let {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
            },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            AsyncImage(
                model = article.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.markmydayicon),
                error = painterResource(R.drawable.markmydayicon)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = article.source?.name ?: "Headlines",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VerticalNewsSection(title: String, state: NewsState) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        when (state) {
            is NewsState.Loading -> {
                repeat(3) { NewsItemShimmer() }
            }
            is NewsState.Success -> {
                state.articles.forEach { article ->
                    NewsListItem(article) {
                        article.url?.let {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                        }
                    }
                }
            }
            is NewsState.Error -> {
                Text(text = state.message, modifier = Modifier.padding(20.dp), color = Color.Gray)
            }
        }
    }
}

@Composable
fun NewsListItem(article: GNewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = article.image,
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.markmydayicon),
                error = painterResource(R.drawable.markmydayicon)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = article.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = article.source?.name ?: "News",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(20.dp)).background(Color.LightGray.copy(alpha = 0.2f)))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(18.dp).background(Color.LightGray.copy(alpha = 0.2f)))
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).background(Color.LightGray.copy(alpha = 0.2f)))
        }
    }
}
